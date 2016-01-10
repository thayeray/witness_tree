import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;



public class DataTableW<T extends Comparable<T>>
{
	private LinkedList<Parcel<T>> table;
	private CountingTree fieldList;
	private CountingTree fieldContentTree;
	private boolean sorted = false;
	
	public DataTableW()
	{	table = new LinkedList<Parcel<T>>();
		fieldList = new CountingTree();
		fieldContentTree = new CountingTree();
	}
	
	public DataTableW(LinkedList<Parcel<T>> table, CountingTree fieldList, CountingTree fieldContentTree)
	{	this.table = table;
		this.fieldList = fieldList;
		this.fieldContentTree = fieldContentTree;
	}
	
	public boolean add(Parcel<T> parcel)
	{	boolean result = table.add(parcel);
		// TODO fill in changes to fieldList and fieldContentTree 
		return result;
	}
	
	/**
	 * A shallow check of equality, that only uses the 'comparator' property of the Parcel.
	 * @param parcel Only needs to have a 'comparator', the data are not checked.
	 * @return True if the comparators are equal.
	 */
	public boolean contains(Parcel<T> parcel)
	{	return table.contains(parcel);
	}
	
	/**
	 * Uses a shallow check of equality, that only uses the 'comparator' property of the Parcel.
	 * @param parcel Only needs to have a 'comparator', the data are not checked.
	 * @return The index of the first matching Parcel.
	 */
	public int indexOf(Parcel<T> parcel)
	{	return table.indexOf(parcel);
	}
	
	public Iterator<Parcel<T>> iterator()
	{	return table.iterator();
	}
	
	/**
	 * 
	 * @return True if there are no parcels in the table
	 */
	public boolean isEmpty()
	{	return table.isEmpty();
	}
	
	/**
	 * 
	 * @return The number of parcels in the table.
	 */
	public int size()
	{	return table.size();
	}
	
	public boolean sort()
	{	boolean result = false;
		try
		{	Collections.sort(table);
			sorted = true;
			result = true;		
		}
		catch(Exception e)
		{	result = false;
		}
		return result;
	}

	/**
	 * The 'join' table's records are added into matching Parcels in the calling table. Parcels that are successfully
	 *   joined are marked as 'joined' = true. The counts of success, no match and failure are returned in an array. 
	 * @param join The table to be combined with. If combining KML and MBL, this should be the KML
	 * @param joinIsKML True if the combine table is KML and the calling table is MBL. False if not combining KML, MBL.
	 * @return Position 0: number of Parcels in the 'join' table that were successfully combined into the calling table.
	 * 	<br>	   Position 1: number of parcels that did not match. 
	 * 	<br>	   Position 2: number of parcels that failed to join because they had different numbers of points. 
	 */
	public int[] combineTables(DataTableW<T> join, boolean joinIsKML)
	{	int currentCount = 0;
		int thisCount = 0;
		int combineCount = 0;
		int failedCount = 0;
		int noMatchCountKML = 0;
		int noMatchCountMBL = 0;
		Iterator<Parcel<T>> joinIter = join.iterator();
		Parcel<T> current = null;
		Iterator<DataRecordW<T>> curIter;
		int matchIndex;
		while(joinIter.hasNext())
		{	current = joinIter.next();	// <-- parcel to be combined
			if(joinIsKML)
				currentCount = current.getGeometryCount() - 1;	// KML has a centroid point that MBL does not.
			else
				currentCount = current.getGeometryCount();
			if(table.contains(current) && current.size() > 0)
			{	matchIndex = table.indexOf(current); 
				thisCount = table.get(matchIndex).getGeometryCount();
				curIter = current.iterator();
				if(thisCount == currentCount)	// successful inner "join"
				{	while(curIter.hasNext())
					{	table.get(matchIndex).add(curIter.next());
					}
					table.get(matchIndex).setCombined(true);
					table.get(matchIndex).setKmlName(current.getKmlName());
					combineCount++;
				}
				else						// failed join, id's match but geometry counts do not. (right "join")
				{	failedCount++;
					current.setFailed(true);
					table.add(current);
				}
			}
			else 
			{	noMatchCountKML++;			// id's do not match, add KML (right "join")
				current.setNoMatchKML(true);
				table.add(current);
			}
		}
		int index;
		for(index = 0;index < table.size();index++)		// go back through and set no match MBL parcels 
		{	if(!table.get(index).isCombined() && !table.get(index).isFailed() && !table.get(index).isNoMatchKML())
			{	table.get(index).setNoMatchMBL(true);	// id's do not match (left "join")
				noMatchCountMBL++;
			}
		}
		return new int[]{combineCount,failedCount,noMatchCountKML,noMatchCountMBL};
	}
	
	public static final int COMBINED_INDEX = 0;
	public static final int FAILED_INDEX = 1;
	public static final int NO_MATCH_KML_INDEX = 2;
	public static final int NO_MATCH_MBL_INDEX = 3;
	
	public LinkedList<Parcel<T>> getTable() {
		return table;
	}

	public void setTable(LinkedList<Parcel<T>> table) {
		this.table = table;
	}

	public CountingTree getFieldList() {
		return fieldList;
	}

	public void setFieldList(CountingTree fieldList) {
		this.fieldList = fieldList;
	}

	public CountingTree getFieldContentTree() {
		return fieldContentTree;
	}

	public void setFieldContentTree(CountingTree fieldContentTree) {
		this.fieldContentTree = fieldContentTree;
	}
	
	public boolean isSorted() {
		return sorted;
	}

	public void setSorted(boolean sorted) {
		this.sorted = sorted;
	}

	/**
	 * Returns a safe copy of the DataTableW
	 * @return A safe copy of the DataTableW
	 */
	public DataTableW<T> clone()
	{	DataTableW<T> DTWcopy = new DataTableW<T>();
		LinkedList<Parcel<T>> tabCopy = new LinkedList<Parcel<T>>();
		for(Parcel<T> parcel:table)
			tabCopy.add(parcel.clone());
		DTWcopy.setTable(tabCopy);
		DTWcopy.setFieldList(fieldList.clone());
		DTWcopy.setFieldContentTree(fieldContentTree.clone());
		return DTWcopy;
	}
	
	public String toString()
	{	String result = "";
		int index, size = table.size();
		for(index = 0; index < size; index++)
			result += table.get(index).toString();
		return result;
	}
}