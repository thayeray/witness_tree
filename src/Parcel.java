
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;

@SuppressWarnings("serial")
public class Parcel<T extends Comparable<T>> extends LinkedList<DataRecordW<T>> implements Comparable<Parcel<T>>, Iterable<DataRecordW<T>>
{	// note that the main data held in this collection is in the extension of the LinkedList --> super 
	private static boolean messagesSilent = true;
	private boolean sorted = false;
	/** True if the parcel has been combined. It has separate MBL and KML records. */
	private boolean combined = false;
	/** True if the parcel is a KML parcel whose comparator matches an MBL parcel, but the geometry counts differ */
	private boolean failed = false;
	/** True if the parcel is a KML parcel whose comparator did not match an MBL parcel */
	private boolean noMatchKML = false;
	/** True if the parcel is a MBL parcel whose comparator did not match an KML parcel */
	private boolean noMatchMBL = false;
	/** True if the parcel has been completely joined. KML fields joined into matching MBL records */
	private boolean joined = false;
	private String kmlName = null;
	private T comparator;
	private int geometryCount = 0;
	
	public Parcel()
	{	super();
		comparator = null;
	}
	
	public Parcel(T comparator)
	{	super();
		this.comparator = comparator;
	}
	
	public Parcel(LinkedList<DataRecordW<T>> fields, T comparator, int geometryCount, String kmlName)
	{	super(fields);
		this.geometryCount = geometryCount;
		this.comparator = comparator;
		this.kmlName = kmlName;
	}
	
	public boolean add(DataRecordW<T> record, boolean isGeometry)
	{	if(isGeometry)
			geometryCount++;
		return super.add(record);
	}
	
	/**
	 * Removes and returns the first field from the parcel.
	 * @return The first field in the parcel.
	 */
	public DataRecordW<T> remove(boolean isGeometry)
	{	if(isGeometry)
			geometryCount--;
		return super.remove();
	}
	
	/**
	 * Removes from the parcel and returns the field at the desired index.
	 * @param index The index of the field to be removed from the parcel.
	 * @return The desired field.
	 */
	public DataRecordW<T> remove(int index, boolean isGeometry)
	{	if(isGeometry)
			geometryCount--;
		return super.remove(index);
	}
	
	public Iterator<DataRecordW<T>> iterator()
	{	return super.iterator();
	}
	
	public boolean isEmpty()
	{	return super.isEmpty();
	}
	
	public DataRecordW<T> poll(boolean isGeometry)
	{	if(isGeometry)
			geometryCount--;
		return super.poll();
	}
	
	public DataRecordW<T> peek()
	{	return super.peek();
	}
	
	public DataRecordW<T> get(int index)
	{	return super.get(index);
	}
	
	/**
	 * 
	 * @param otherParcel The parcel to which you want to compare the calling parcel
	 * @return The value for the comparison of the two parcels' 'comparator' properties.
	 */
	public int compareTo(Parcel<T> otherParcel)
	{	try
		{
			return comparator.compareTo(otherParcel.getComparator());
		}
		catch(Exception e)
		{	return 0;
		}
	}
	
	public boolean contains(DataRecordW<T> havingTheseCompareOnValues)
	{	return super.contains(havingTheseCompareOnValues);
	}
	
	public int size()
	{	return super.size();
	}
	
	public boolean sort()
	{	boolean result = false;
		try
		{	Collections.sort(this);
			sorted = true;		
		}
		catch(Exception e)
		{	result = false;		
		}
		return result;
	}
	
	public static boolean areMessagesSilent() {
		return messagesSilent;
	}

	public static void setMessagesSilent(boolean messagesSilent) {
		Parcel.messagesSilent = messagesSilent;
	}

	/**
	 * Returns the value that the compareTo method uses for comparison.
	 * @return The comparator value
	 */
	public T getComparator()
	{	return comparator;
	}
	
	public boolean setComparator(T comparator)
	{	if (comparator instanceof Comparable)
		{	this.comparator = comparator;
			return true;
		}
		else
			return false;
	}
	
	public int getGeometryCount()
	{	return geometryCount;
	}
	
	public void setGeometryCount(int geometryCount)
	{	this.geometryCount = geometryCount;
	}
	
	/** True if the parcel has been sorted */
	public boolean isSorted() {
		return sorted;
	}

	/** True if the parcel has been sorted */
	public void setSorted(boolean sorted) {
		this.sorted = sorted;
	}

	/** True if the parcel has been combined. It has separate MBL and KML records. */
	public boolean isCombined() {
		return combined;
	}

	/** True if the parcel has been combined. It has separate MBL and KML records. */
	public void setCombined(boolean combined) {
		this.combined = combined;
	}

	/** True if the parcel has been completely joined. KML fields joined into matching MBL records */
	public boolean isJoined() {
		return joined;
	}

	/** True if the parcel has been completely joined. KML fields joined into matching MBL records */
	public void setJoined(boolean joined) {
		this.joined = joined;
	}

	/** True if the parcel is a KML parcel whose comparator matches an MBL parcel, but the geometry counts differ */
	public boolean isFailed() {
		return failed;
	}

	/** True if the parcel is a KML parcel whose comparator matches an MBL parcel, but the geometry counts differ */
	public void setFailed(boolean failed) {
		this.failed = failed;
	}

	/** True if the parcel is a KML parcel whose comparator did not match an MBL parcel */
	public boolean isNoMatchKML() {
		return noMatchKML;
	}

	/** True if the parcel is a KML parcel whose comparator did not match an MBL parcel */
	public void setNoMatchKML(boolean noMatchKML) {
		this.noMatchKML = noMatchKML;
	}

	/** True if the parcel is a MBL parcel whose comparator did not match an KML parcel */
	public boolean isNoMatchMBL() {
		return noMatchMBL;
	}

	/** True if the parcel is a MBL parcel whose comparator did not match an KML parcel */
	public void setNoMatchMBL(boolean noMatchMBL) {
		this.noMatchMBL = noMatchMBL;
	}

	public static boolean isMessagesSilent() {
		return messagesSilent;
	}

	public String getKmlName() {
		return kmlName;
	}

	public void setKmlName(String kmlName) {
		this.kmlName = kmlName;
	}

	public Parcel<T> clone()
	{	LinkedList<DataRecordW<T>> copy = new LinkedList<DataRecordW<T>>();
		int index;
		for(index = 0; index < this.size(); index++)
			copy.add(super.get(index).clone());
		return new Parcel<T>(copy,getComparator(),getGeometryCount(),getKmlName());
	}
	
	/**
	 * This equals method only checks if the 'comparator' parameters are the same between Parcel instances.  
	 * @param otherParcel The Parcel to be checked against.
	 * @return True if the comparators are the same. False if not, or not an instanceof Parcel, or any other Exception is created. 
	 */
	@SuppressWarnings("unchecked")
	public boolean equals(Object otherParcel)
	{	boolean result = false;
		if(otherParcel instanceof Parcel<?>)	
		{	try
			{	result = this.getComparator().equals(((Parcel<T>) otherParcel).getComparator());}
			catch(Exception e)
			{	result = false;}
		}
		return result; 
	}
	
	public String toString()
	{	String result = "";
		if (!messagesSilent && sorted)
			result += "The parcel is sorted, the comparator is: "+ comparator +" \n";
		else if (!messagesSilent)
			result += "The parcel is unsorted, the comparator is: "+ comparator +" \n";
		int index;
		for(index = 0; index < this.size(); index++)
			result += super.get(index) + "\n";
		return result;
	}
	
	public static void main(String[] args)
	{	Parcel<String> test = new Parcel<String>("alpha");
		test.add(new DataRecordW<String>(new String[]{"most","have","a","lost","love"},0,0,1));
		test.add(new DataRecordW<String>(new String[]{"big","bad","wolf","is","coming"},0,0,1));
		test.add(new DataRecordW<String>(new String[]{"most","is","gone","too","bad"},0,0,1));
		test.add(new DataRecordW<String>(new String[]{"Mary","me","my","dear","love"},0,0,1));
		
		Parcel<String> test2 = new Parcel<String>("beta");
		test2.add(new DataRecordW<String>(new String[]{"I","am","a","rock","land"},0,0));
		test2.add(new DataRecordW<String>(new String[]{"run","to","the","far","side"},0,0));
		test2.add(new DataRecordW<String>(new String[]{"Oh","I","did","not","think"},0,0));
		test2.add(new DataRecordW<String>(new String[]{"run","for","me","then","so"},0,0));

		Parcel.setMessagesSilent(false);
		System.out.println("alpha compared to beta: " + test.compareTo(test2) 
				+ "\nbeta compared to alpha: " + test2.compareTo(test) + "\n");
		System.out.println(test);
		test.sort();
		System.out.println(test);
		System.out.println(test2);
		test2.sort();
		System.out.println(test2);
		
	}
}