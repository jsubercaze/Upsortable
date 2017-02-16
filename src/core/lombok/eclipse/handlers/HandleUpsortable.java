/*
 * Copyright (C) 2009-2014 The Project Lombok Authors.
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package lombok.eclipse.handlers;

import static lombok.core.handlers.HandlerUtil.*;
import static lombok.eclipse.Eclipse.*;
import static lombok.eclipse.handlers.EclipseHandlerUtil.*;
import static lombok.eclipse.handlers.EclipseHandlerUtil.toAllSetterNames;
import static lombok.eclipse.handlers.EclipseHandlerUtil.toSetterName;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.internal.compiler.ast.ASTNode;
import org.eclipse.jdt.internal.compiler.ast.AllocationExpression;
import org.eclipse.jdt.internal.compiler.ast.Annotation;
import org.eclipse.jdt.internal.compiler.ast.Argument;

import org.eclipse.jdt.internal.compiler.ast.ArrayAllocationExpression;
import org.eclipse.jdt.internal.compiler.ast.ArrayInitializer;
import org.eclipse.jdt.internal.compiler.ast.ArrayTypeReference;

import org.eclipse.jdt.internal.compiler.ast.ArrayReference;
import org.eclipse.jdt.internal.compiler.ast.Assignment;
import org.eclipse.jdt.internal.compiler.ast.Block;
import org.eclipse.jdt.internal.compiler.ast.BinaryExpression;
import org.eclipse.jdt.internal.compiler.ast.EqualExpression;
import org.eclipse.jdt.internal.compiler.ast.Expression;
import org.eclipse.jdt.internal.compiler.ast.FieldDeclaration;
import org.eclipse.jdt.internal.compiler.ast.ForeachStatement;
import org.eclipse.jdt.internal.compiler.ast.IfStatement;
import org.eclipse.jdt.internal.compiler.ast.ImportReference;
import org.eclipse.jdt.internal.compiler.ast.LocalDeclaration;
import org.eclipse.jdt.internal.compiler.ast.MessageSend;
import org.eclipse.jdt.internal.compiler.ast.MethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.NameReference;
import org.eclipse.jdt.internal.compiler.ast.OperatorIds;
import org.eclipse.jdt.internal.compiler.ast.ParameterizedSingleTypeReference;
import org.eclipse.jdt.internal.compiler.ast.PostfixExpression;
import org.eclipse.jdt.internal.compiler.ast.ReturnStatement;
import org.eclipse.jdt.internal.compiler.ast.SingleNameReference;
import org.eclipse.jdt.internal.compiler.ast.SingleTypeReference;
import org.eclipse.jdt.internal.compiler.ast.Statement;
import org.eclipse.jdt.internal.compiler.ast.StringLiteral;
import org.eclipse.jdt.internal.compiler.ast.ThisReference;
import org.eclipse.jdt.internal.compiler.ast.TrueLiteral;
import org.eclipse.jdt.internal.compiler.ast.TryStatement;
import org.eclipse.jdt.internal.compiler.ast.TypeDeclaration;
import org.eclipse.jdt.internal.compiler.ast.TypeReference;
import org.eclipse.jdt.internal.compiler.ast.UnaryExpression;
import org.eclipse.jdt.internal.compiler.ast.WhileStatement;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants;
import org.eclipse.jdt.internal.compiler.lookup.TypeIds;
import org.mangosdk.spi.ProviderFor;

import lombok.AccessLevel;
import lombok.ConfigurationKeys;
import lombok.Setter;
import lombok.Upsortable;
import lombok.core.AST.Kind;
import lombok.core.AnnotationValues;
import lombok.eclipse.EclipseAnnotationHandler;
import lombok.eclipse.EclipseNode;
import lombok.eclipse.handlers.EclipseHandlerUtil.FieldAccess;

/**
 * Handles the {@code lombok.Setter} annotation for eclipse.
 */
@ProviderFor(EclipseAnnotationHandler.class) public class HandleUpsortable extends EclipseAnnotationHandler<Upsortable> {
	public boolean generateSetterForType(EclipseNode typeNode, EclipseNode pos, AccessLevel level, boolean checkForTypeLevelSetter) {
		if (checkForTypeLevelSetter) {
			if (hasAnnotation(Setter.class, typeNode)) {
				// The annotation will make it happen, so we can skip it.
				return true;
			}
		}
		
		TypeDeclaration typeDecl = null;
		if (typeNode.get() instanceof TypeDeclaration) typeDecl = (TypeDeclaration) typeNode.get();
		int modifiers = typeDecl == null ? 0 : typeDecl.modifiers;
		boolean notAClass = (modifiers & (ClassFileConstants.AccInterface | ClassFileConstants.AccAnnotation | ClassFileConstants.AccEnum)) != 0;
		
		if (typeDecl == null || notAClass) {
			pos.addError("@Setter is only supported on a class or a field.");
			return false;
		}
		
		for (EclipseNode field : typeNode.down()) {
			if (field.getKind() == Kind.METHOD) {
				System.out.println(field);
			}
			if (field.getKind() != Kind.FIELD) continue;
			FieldDeclaration fieldDecl = (FieldDeclaration) field.get();
			if (!filterField(fieldDecl)) continue;
			// Skip final fields.
			if ((fieldDecl.modifiers & ClassFileConstants.AccFinal) != 0) continue;
			
			generateSetterForField(field, pos, level);
		}
		return true;
	}
	
	/**
	 * Generates a setter on the stated field.
	 * 
	 * Used by {@link HandleData}.
	 * 
	 * The difference between this call and the handle method is as follows:
	 * 
	 * If there is a {@code lombok.Setter} annotation on the field, it is used
	 * and the same rules apply (e.g. warning if the method already exists,
	 * stated access level applies). If not, the setter is still generated if it
	 * isn't already there, though there will not be a warning if its already
	 * there. The default access level is used.
	 */
	public void generateSetterForField(EclipseNode fieldNode, EclipseNode sourceNode, AccessLevel level) {
		if (hasAnnotation(Setter.class, fieldNode)) {
			// The annotation will make it happen, so we can skip it.
			return;
		}
		
		List<Annotation> empty = Collections.emptyList();
		
		createSetterForField(level, fieldNode, sourceNode, false, empty, empty);
	}
	
	public void handle(AnnotationValues<Upsortable> annotation, Annotation ast, EclipseNode annotationNode) {
		handleFlagUsage(annotationNode, ConfigurationKeys.SETTER_FLAG_USAGE, "@Setter");
		
		EclipseNode node = annotationNode.up();
		// Inject the required imports
		EclipseNode topNode = node.top();
		addImports(topNode);
		// Inject the UpsortableValue interface
		addInterface(node);
		AccessLevel level = annotation.getInstance().value();
		if (level == AccessLevel.NONE || node == null) return;
		
		List<Annotation> onMethod = unboxAndRemoveAnnotationParameter(ast, "onMethod", "@Setter(onMethod=", annotationNode);
		List<Annotation> onParam = unboxAndRemoveAnnotationParameter(ast, "onParam", "@Setter(onParam=", annotationNode);
		
		switch (node.getKind()) {
		case FIELD:
			createSetterForFields(level, annotationNode.upFromAnnotationToFields(), annotationNode, true, onMethod, onParam);
			break;
		case TYPE:
			if (!onMethod.isEmpty()) {
				annotationNode.addError("'onMethod' is not supported for @Setter on a type.");
			}
			if (!onParam.isEmpty()) {
				annotationNode.addError("'onParam' is not supported for @Setter on a type.");
			}
			generateSetterForType(node, annotationNode, level, false);
			break;
		}
		int a = 3;
	}
	
	private void addInterface(EclipseNode node) {
		ASTNode source = node.get();
		int pS = source.sourceStart, pE = source.sourceEnd;
		long p = (long) pS << 32 | pE;
		// char[][] importToToken = importToToken("lombok.UpsortableValue");
		SingleTypeReference interfaceRef = new SingleTypeReference("UpsortableValue".toCharArray(), p);
		injectInterface(node, interfaceRef);
	}
	
	private void addImports(EclipseNode topNode) {
		ASTNode source = topNode.get();
		int pS = source.sourceStart, pE = source.sourceEnd;
		long p = (long) pS << 32 | pE;
		char[][] importToToken = importToToken("java.util.Set");
		ImportReference setRef = new ImportReference(importToToken, new long[] {pS, pE}, false, 0);
		injectImport(topNode, setRef);
		importToToken = importToToken("lombok.UpsortableSets");
		setRef = new ImportReference(importToToken, new long[] {pS, pE}, false, 0);
		injectImport(topNode, setRef);
		importToToken = importToToken("lombok.UpsortableSet");
		setRef = new ImportReference(importToToken, new long[] {pS, pE}, false, 0);
		injectImport(topNode, setRef);
		importToToken = importToToken("lombok.UpsortableValue");
		setRef = new ImportReference(importToToken, new long[] {pS, pE}, false, 0);
		injectImport(topNode, setRef);
	}
	
	private char[][] importToToken(String importName) {
		String[] tokenized = importName.split("\\.");
		char[][] tokens = new char[tokenized.length][];
		int i = 0;
		for (String cs : tokenized) {
			tokens[i] = tokenized[i].toCharArray();
			i++;
		}
		
		return tokens;
		
	}
	
	public void createSetterForFields(AccessLevel level, Collection<EclipseNode> fieldNodes, EclipseNode sourceNode, boolean whineIfExists, List<Annotation> onMethod, List<Annotation> onParam) {
		for (EclipseNode fieldNode : fieldNodes) {
			createSetterForField(level, fieldNode, sourceNode, whineIfExists, onMethod, onParam);
		}
	}
	
	public void createSetterForField(AccessLevel level, EclipseNode fieldNode, EclipseNode sourceNode, boolean whineIfExists, List<Annotation> onMethod, List<Annotation> onParam) {
		
		ASTNode source = sourceNode.get();
		if (fieldNode.getKind() != Kind.FIELD) {
			sourceNode.addError("@Setter is only supported on a class or a field.");
			return;
		}
		
		FieldDeclaration field = (FieldDeclaration) fieldNode.get();
		TypeReference fieldType = copyType(field.type, source);
		boolean isBoolean = isBoolean(fieldType);
		String setterName = toSetterName(fieldNode, isBoolean);
		boolean shouldReturnThis = shouldReturnThis(fieldNode);
		
		if (setterName == null) {
			fieldNode.addWarning("Not generating setter for this field: It does not fit your @Accessors prefix list.");
			return;
		}
		
		int modifier = toEclipseModifier(level) | (field.modifiers & ClassFileConstants.AccStatic);
		
		for (String altName : toAllSetterNames(fieldNode, isBoolean)) {
			switch (methodExists(altName, fieldNode, false, 1)) {
			case EXISTS_BY_LOMBOK:
				return;
			case EXISTS_BY_USER:
				if (whineIfExists) {
					String altNameExpl = "";
					if (!altName.equals(setterName)) altNameExpl = String.format(" (%s)", altName);
					fieldNode.addWarning(String.format("Not generating %s(): A method with that name already exists%s", setterName, altNameExpl));
				}
				return;
			default:
			case NOT_EXISTS:
				// continue scanning the other alt names.
			}
		}
		
		MethodDeclaration method = createSetter((TypeDeclaration) fieldNode.up().get(), fieldNode, setterName, shouldReturnThis, modifier, sourceNode, onMethod, onParam);
		injectMethod(fieldNode.up(), method);
	}
	
	static MethodDeclaration createSetter(TypeDeclaration parent, EclipseNode fieldNode, String name, boolean shouldReturnThis, int modifier, EclipseNode sourceNode, List<Annotation> onMethod, List<Annotation> onParam) {
		FieldDeclaration field = (FieldDeclaration) fieldNode.get();
		ASTNode source = sourceNode.get();
		int pS = source.sourceStart, pE = source.sourceEnd;
		long p = (long) pS << 32 | pE;
		MethodDeclaration method = new MethodDeclaration(parent.compilationResult);
		method.modifiers = modifier;
		if (shouldReturnThis) {
			method.returnType = cloneSelfType(fieldNode, source);
		}
		
		if (method.returnType == null) {
			method.returnType = TypeReference.baseTypeReference(TypeIds.T_void, 0);
			method.returnType.sourceStart = pS;
			method.returnType.sourceEnd = pE;
			shouldReturnThis = false;
		}
		Annotation[] deprecated = null;
		if (isFieldDeprecated(fieldNode)) {
			deprecated = new Annotation[] {generateDeprecatedAnnotation(source)};
		}
		method.annotations = copyAnnotations(source, onMethod.toArray(new Annotation[0]), deprecated);
		Argument param = new Argument(field.name, p, copyType(field.type, source), Modifier.FINAL);
		param.sourceStart = pS;
		param.sourceEnd = pE;
		method.arguments = new Argument[] {param};
		method.selector = name.toCharArray();
		method.binding = null;
		method.thrownExceptions = null;
		method.typeParameters = null;
		method.bits |= ECLIPSE_DO_NOT_TOUCH_FLAG;
		Expression fieldRef = createFieldAccessor(fieldNode, FieldAccess.ALWAYS_FIELD, source);
		NameReference fieldNameRef = new SingleNameReference(field.name, p);
		Assignment assignment = new Assignment(fieldRef, fieldNameRef, (int) p);
		assignment.sourceStart = pS;
		assignment.sourceEnd = assignment.statementEnd = pE;
		method.bodyStart = method.declarationSourceStart = method.sourceStart = source.sourceStart;
		method.bodyEnd = method.declarationSourceEnd = method.sourceEnd = source.sourceEnd;
		
		Annotation[] nonNulls = findAnnotations(field, NON_NULL_PATTERN);
		Annotation[] nullables = findAnnotations(field, NULLABLE_PATTERN);
		List<Statement> statements = new ArrayList<Statement>(5);
		
		/*
		 * if (this.$field == $field) return;
		 */ {
			Expression lfRef = createFieldAccessor(fieldNode, FieldAccess.ALWAYS_FIELD, source);
			setGeneratedBy(lfRef, source);
			NameReference fRef = new SingleNameReference(field.name, p);
			setGeneratedBy(lfRef, source);
			setGeneratedBy(fRef, source);
			EqualExpression otherEqualsThis = new EqualExpression(lfRef, fRef, OperatorIds.EQUAL_EQUAL);
			setGeneratedBy(otherEqualsThis, source);
			
			TrueLiteral trueLiteral = new TrueLiteral(pS, pE);
			setGeneratedBy(trueLiteral, source);
			ReturnStatement returnTrue = new ReturnStatement(null, pS, pE);
			setGeneratedBy(returnTrue, source);
			IfStatement ifOtherEqualsThis = new IfStatement(otherEqualsThis, returnTrue, pS, pE);
			setGeneratedBy(ifOtherEqualsThis, source);
			statements.add(ifOtherEqualsThis);
		}
		
		/*
		 * Create the statements within the try/catch block
		 */
		
		/*
		 * String fname = this.getClass().getDeclaredField("$field").getName();
		 */
		LocalDeclaration fieldClass = new LocalDeclaration("fname".toCharArray(), pS, pE);
		{
			// Left side
			fieldClass.modifiers |= Modifier.FINAL;
			fieldClass.type = new SingleTypeReference("String".toCharArray(), p);
			setGeneratedBy(fieldClass, source);
			// Right side - first call
			MessageSend getClass = new MessageSend();
			getClass.sourceStart = pS;
			getClass.sourceEnd = pE;
			setGeneratedBy(getClass, source);
			ThisReference thisReference = new ThisReference(pS, pE);
			setGeneratedBy(thisReference, source);
			getClass.receiver = thisReference;
			getClass.selector = "getClass".toCharArray();
			// Right side - second call
			MessageSend getDeclaredField = new MessageSend();
			getDeclaredField.sourceStart = pS;
			getDeclaredField.sourceEnd = pE;
			setGeneratedBy(getDeclaredField, source);
			getDeclaredField.receiver = getClass;
			getDeclaredField.selector = "getDeclaredField".toCharArray();
			// Arguments of the call
			getDeclaredField.arguments = new Expression[] {new StringLiteral(field.name, pS, pE, 0)};
			// Third call
			MessageSend getName = new MessageSend();
			getName.sourceStart = pS;
			getName.sourceEnd = pE;
			setGeneratedBy(getName, source);
			getName.receiver = getDeclaredField;
			getName.selector = "getName".toCharArray();
			// Assign right to left
			fieldClass.initialization = getName;
			// Do not add this goes into try catch statement
			// statements.add(fieldClass);
			
		}
		
		// @formatter:off
		/*
		 * Set<UpsortableSet> set =
		 * UpsortableSets.getGlobalUpsortable().get(this.getClass().getName() +
		 * "." + fname);
		 */
		LocalDeclaration theSet = new LocalDeclaration("set".toCharArray(), pS, pE);
		{
			/****** this.getClass().getName() ****/
			// Right side - first call
			MessageSend getClass = new MessageSend();
			getClass.sourceStart = pS;
			getClass.sourceEnd = pE;
			setGeneratedBy(getClass, source);
			ThisReference thisReference = new ThisReference(pS, pE);
			setGeneratedBy(thisReference, source);
			getClass.receiver = thisReference;
			getClass.selector = "getClass".toCharArray();
			// Right side - second call
			MessageSend getName = new MessageSend();
			getName.sourceStart = pS;
			getName.sourceEnd = pE;
			setGeneratedBy(getName, source);
			getName.receiver = getClass;
			getName.selector = "getName".toCharArray();
			/****** END this.getClass().getName() ****/
			
			/****** field.getName() *****/
			char[] fnameVariableName = "fname".toCharArray();
			SingleNameReference fnameReference = new SingleNameReference(fnameVariableName, p);
			setGeneratedBy(fnameReference, source);
			/****** END field.getName() ****/
			
			/****** this.getClass().getName() + "." + field.getName()) ******/
			final int PLUS = OperatorIds.PLUS;
			char[] concat = ".".toCharArray();
			Expression current = new StringLiteral(concat, pS, pE, 0);
			
			current = new BinaryExpression(getName, current, PLUS);
			setGeneratedBy(current, source);
			current = new BinaryExpression(current, fnameReference, PLUS);
			setGeneratedBy(current, source);
			/******
			 * END this.getClass().getName() + "." + field.getName())
			 ******/
			
			/****** UpsortableSets.getGlobalUpsortable().get( ******/
			NameReference upsortableSetsClass = HandleToString.generateQualifiedNameRef(source, "lombok".toCharArray(), "UpsortableSets".toCharArray());
			
			// UpsortableSets.getGlobalUpsortable()
			MessageSend getGlobalUpsortable = new MessageSend();
			getGlobalUpsortable.sourceStart = pS;
			getGlobalUpsortable.sourceEnd = pE;
			setGeneratedBy(getGlobalUpsortable, source);
			getGlobalUpsortable.receiver = upsortableSetsClass;
			getGlobalUpsortable.selector = "getGlobalUpsortable".toCharArray();
			
			MessageSend lastGet = new MessageSend();
			lastGet.sourceStart = pS;
			lastGet.sourceEnd = pE;
			setGeneratedBy(lastGet, source);
			lastGet.receiver = getGlobalUpsortable;
			lastGet.selector = "get".toCharArray();
			lastGet.arguments = new Expression[] {current};
			
			/****** END UpsortableSets.getGlobalUpsortable().get() ******/
			
			// Left side
			ParameterizedSingleTypeReference pstr = new ParameterizedSingleTypeReference("Set".toCharArray(), new TypeReference[] {new SingleTypeReference("UpsortableSet".toCharArray(), p)}, 0, p);
			
			theSet.modifiers |= Modifier.FINAL;
			theSet.type = pstr;
			
			theSet.initialization = lastGet;
			
		}
		/*
		 * UpsortableSet[] participatingSets = new UpsortableSet[set.size()];
		 */
		{
			ArrayTypeReference part = new ArrayTypeReference("UpsortableSet".toCharArray(), 1, p);
			LocalDeclaration participatingSets = new LocalDeclaration("participatingSets".toCharArray(), pS, pE);
			participatingSets.type = part;
			// Create the new Array
			ArrayAllocationExpression allocationStatement = new ArrayAllocationExpression();
			allocationStatement.type = new SingleTypeReference("UpsortableSet".toCharArray(), p);
			
			MessageSend send = new MessageSend();
			send.receiver = new SingleNameReference("set".toCharArray(), p);
			send.selector = "size".toCharArray();
			// Init the array
			ArrayInitializer fieldNames = new ArrayInitializer();
			fieldNames.sourceStart = pS;
			fieldNames.sourceEnd = pE;
			fieldNames.expressions = new Expression[] {send};
			
			// allocationStatement. = new Expression[] {send};
			allocationStatement.dimensions = new Expression[] {send};
			participatingSets.initialization = allocationStatement;
			int a = 3;
		}

		
		/*
		 * int found =0;
		 */
		LocalDeclaration found = new LocalDeclaration("found".toCharArray(), pS, pE);
		{
			found.sourceStart = pS;
			found.sourceEnd = pE;
			TypeReference baseTypeReference = TypeReference.baseTypeReference(TypeIds.T_int, 0);
			found.type = baseTypeReference;
			found.initialization = makeIntLiteral("0".toCharArray(), source);
		}
		
		/*
		 * for(UpsortableSet<?> upsortableSet : set){
		 * 
		 */
		ForeachStatement foreach = null;
		{
			LocalDeclaration upsortableSet = new LocalDeclaration("upsortableSet".toCharArray(), pS, pE);
			SingleNameReference set = new SingleNameReference("set".toCharArray(), p);
			upsortableSet.type =  new ParameterizedSingleTypeReference("UpsortableSet".toCharArray(),
					new TypeReference[] {new SingleTypeReference("?".toCharArray(), p)}, 0, p);
			foreach = new ForeachStatement(upsortableSet, pS);
//			new LocalVariableb
//			foreach.collectionVariable = set;
			foreach.collection=set;
			//foreach.scope= new Blc
		}
		
		/*
		 * if(upsortableSet.remove(this))
		 *    participatingSets[found++] = upsortableSet;
		 */
		IfStatement ifstmt;
		{
			MessageSend remove = new MessageSend();
			remove.sourceStart = pS;
			remove.sourceEnd = pE;
			setGeneratedBy(remove, source);
			remove.receiver= new SingleNameReference("upsortableSet".toCharArray(), p);
			remove.selector = "get".toCharArray();
			remove.arguments = new Expression[] {new ThisReference(pS, pE)};
			
			
			//Inside the IF
			Block block = new Block(0);
			 SingleNameReference foundL = new SingleNameReference("found".toCharArray(), p);
			 SingleNameReference foundR = new SingleNameReference("found".toCharArray(), p);
			 BinaryExpression plusOne = new BinaryExpression(foundR,  makeIntLiteral("0".toCharArray(), source), OperatorIds.PLUS);
			 Assignment as = new Assignment(foundL, plusOne, pE);
			//Array ref
			SingleNameReference expression = new SingleNameReference("found".toCharArray(), p);
			SingleNameReference arrayRef = new SingleNameReference("participatingSets".toCharArray(), p);
			ArrayReference arrayReference = new ArrayReference(arrayRef, expression);
			arrayReference.receiver =  new SingleNameReference("upsortableSet".toCharArray(), p);
			block.sourceStart=pS;block.sourceEnd=pE;
			block.statements = new Statement[]{as,arrayReference};
			 
			//IF
			ifstmt = new IfStatement(remove, block, pS, pE); //FIXME replace found by 'participating[....
			
			foreach.action=ifstmt;
		}
		
	
		TryStatement tryStatement = new TryStatement();
		setGeneratedBy(tryStatement, source);
		tryStatement.tryBlock = new Block(0);
		// Positions for in-method generated nodes are special
		tryStatement.tryBlock.sourceStart = pS;
		tryStatement.tryBlock.sourceEnd = pE;
		setGeneratedBy(tryStatement.tryBlock, source);
		Argument catchArg = new Argument("e".toCharArray(), pE, new SingleTypeReference("NoSuchFieldException".toCharArray(), p), Modifier.FINAL);
		setGeneratedBy(catchArg, source);
		catchArg.declarationSourceEnd = catchArg.declarationEnd = catchArg.sourceEnd = pE;
		catchArg.declarationSourceStart = catchArg.modifiersSourceStart = catchArg.sourceStart = pE;
		tryStatement.catchArguments = new Argument[] {catchArg};
		Block block = new Block(0);
		block.sourceStart = pS;
		block.sourceEnd = pE;
		setGeneratedBy(block, source);
		block.statements = new Statement[] {};
		tryStatement.tryBlock.statements = new Statement[] {fieldClass, theSet, found,foreach};
		tryStatement.catchBlocks = new Block[] {block};
		statements.add(tryStatement);
		
		method.statements = statements.toArray(new Statement[0]);
		param.annotations = copyAnnotations(source, nonNulls, nullables, onParam.toArray(new Annotation[0]));
		
		method.traverse(new SetGeneratedByVisitor(source), parent.scope);
		return method;
	}
	
	// @formatter:off
		/**
		 * // Fail fast if (this.date == newDate) { return; } // Get the list of registered set DONE 
		 * 
		 * try {
		 * 
		 * 		final String fname = this.getClass().getDeclaredField("foo").getName(); DONE
		 * 
		 * 		Set<UpsortableSet> set = UpsortableSets.getGlobalUpsortable().get(this.getClass().getName() + "." + fname);
		 * 
		 * 		UpsortableSet[] participatingSets = new UpsortableSet[set.size()];
		 * 
		 * 		int found = 0;
		 * 		for (UpsortableSet<?> upsortableSet : set) {
		 * 				if (upsortableSet.remove(this)) {
		 * 					participatingSets[found] = upsortableSet;
							found = found+1;
		 * 		 	}
		 * 		} 
		 * 
		 * 		// Update value
		 * 		this.$field = newDate;
		 * 
		 * 		int i = 0;
		 *   	while(i<found){
		 *			participatingSets[i].add(this);
		 *		 	i=i+1;
		 *      }
			
		 * } catch (Exception e) {
		 * 		e.printStackTrace();
		 * } // method end
		 */
		// @formatter:on
}
