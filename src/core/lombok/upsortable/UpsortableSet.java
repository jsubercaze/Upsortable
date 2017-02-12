package lombok.upsortable;

import java.util.SortedSet;

/**
 * Restrict all operations to instances of {@link UpsortableValue}
 * 
 * @author Julien
 *
 * @param <E>
 */
@SuppressWarnings("rawtypes")
public interface UpsortableSet<E extends UpsortableValue> extends SortedSet<E> {
	 
	public Class getEntryType();
	
	public void setEntryType(Class entryType);
}
