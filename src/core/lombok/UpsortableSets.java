package lombok;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicInteger;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;
import javassist.bytecode.InstructionPrinter;

/**
 * Support creations of {@link UpsortableSet} for both {@link TreeSet} and
 * {@link ConcurrentSkipListSet}, the two Java API implementations of
 * {@link SortedSet}.
 * 
 * @author Julien Subercaze
 *
 */
@SuppressWarnings("rawtypes")
public class UpsortableSets {

	/**
	 * Static counter to number the sets
	 */
	static final AtomicInteger counter = new AtomicInteger();

	/**
	 * Global structure that stores the mapping between
	 * <code> Class -> Fields -> Sets where they take part into comparator</code>
	 */

	public final static Map<String, Set<UpsortableSet>> GLOBAL_UPSORTABLE = new HashMap<String, Set<UpsortableSet>>();

	public static <E extends UpsortableValue> UpsortableSet<E> newUpsortableTreeSet(Comparator<? super E> comparator) {
		return new UpsortableTreeSet<E>(comparator);
	}

	public static <E extends UpsortableValue> UpsortableSet<E> newConcurrentSkipListSet(
			Comparator<? super E> comparator) {
		return new UpsortableConcurrentSkipListSet<E>(comparator);
	}

	static synchronized void init(Comparator<?> comparator, UpsortableSet<?> upsortableSet) {

		try {

			Type genericSuperclass = comparator.getClass().getGenericInterfaces()[0];
			upsortableSet.setEntryType(((Class) ((ParameterizedType) genericSuperclass).getActualTypeArguments()[0]));
		} catch (Exception e) {
			e.printStackTrace();
		}
		// Parse the field from the comparator and store them into the
		// global static map
		ClassPool pool = ClassPool.getDefault();
		try {
			CtClass cc = pool.get(comparator.getClass().getName());
			CtClass[] params = new CtClass[2];
			params[0] = pool.get(upsortableSet.getEntryType().getName());
			params[1] = pool.get(upsortableSet.getEntryType().getName());
			CtMethod m = cc.getDeclaredMethod("compare", params);
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			PrintStream ps = new PrintStream(baos);
			InstructionPrinter i = new InstructionPrinter(ps);
			i.print(m);
			String content = baos.toString();
			// Important point here, by checking the bytecore, we are
			// able to support both direct and getters access
			for (String line : content.split("\\r?\\n")) {
				if (line.contains("getfield")) {
					String val = line.split(" Field ", 2)[1].split("\\(", 2)[0];
					Set<UpsortableSet> set = null;
					if ((set = GLOBAL_UPSORTABLE.get(val)) != null) {
						set.add(upsortableSet);
					} else {
						// Provide synchronized set for thread safety
						// Even if this method is synchronized, it requires set
						// level synchro for the case where a thread is creating
						// a new set with a new comparator while another thread
						// is accessing through annotated setter
						set = new HashSet<UpsortableSet>();
						set = Collections.synchronizedSet(set);
						set.add(upsortableSet);
						GLOBAL_UPSORTABLE.put(val, set);

					}
				}
			}
		} catch (NotFoundException e) {
			e.printStackTrace();
		}
	}

	static class UpsortableConcurrentSkipListSet<E extends UpsortableValue> extends ConcurrentSkipListSet<E>
			implements UpsortableSet<E> {
		/**
		 * Type of entries in this set
		 */
		Class entryType;
		/**
		 * For hashcode and equals
		 */
		int number = counter.incrementAndGet();

		private static final long serialVersionUID = 787720712778371596L;

		public UpsortableConcurrentSkipListSet(Comparator<? super E> comparator) {
			super(comparator);
			// Update the global structure
			init(comparator, this);

		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = super.hashCode();
			result = prime * result + number;
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (!super.equals(obj))
				return false;
			if (getClass() != obj.getClass())
				return false;
			UpsortableTreeSet other = (UpsortableTreeSet) obj;
			if (number != other.number)
				return false;
			return true;
		}

		public Class getEntryType() {
			return entryType;
		}

		public void setEntryType(Class entryType) {
			this.entryType = entryType;
		}

	}

	static class UpsortableTreeSet<E extends UpsortableValue> extends TreeSet<E> implements UpsortableSet<E> {
		/**
		 * Type of entries in this set
		 */
		Class entryType;
		/**
		 * For hashcode and equals
		 */
		int number = counter.incrementAndGet();

		private static final long serialVersionUID = 7877207082764371596L;

		public UpsortableTreeSet(Comparator<? super E> comparator) {
			super(comparator);
			// Update the global structure
			init(comparator, this);

		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = super.hashCode();
			result = prime * result + number;
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (!super.equals(obj))
				return false;
			if (getClass() != obj.getClass())
				return false;
			UpsortableTreeSet other = (UpsortableTreeSet) obj;
			if (number != other.number)
				return false;
			return true;
		}

		public Class getEntryType() {
			return entryType;
		}

		public void setEntryType(Class entryType) {
			this.entryType = entryType;
		}

	}
}
