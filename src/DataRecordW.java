/**
 * 	Thayer Young	4/19/2014	CCBC CSIT 211 Project 3: DataKnife: DataRecord.java 
 */


import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * Objects of the DataRecord class each hold a single record from a database table.  As a record holds data in
 *  multiple fields, the data are stored as separate entries in an ArrayList of generic type T.  The compareTo
 *  method can be set to any one of up to three key fields, or set to use a prioritized list of those key fields.
 * @author Thayer A. Young
 * @param <T> The generic data type that objects of this class will hold.  The specific type should be declared when 
 *  a DataRecord object is instantiated.
 * @version 1.0 4/19/2014
 */
public class DataRecordW<T extends Comparable<T>> implements Comparable<DataRecordW<T>>, Iterable<T>
{
	private static int compareOn;
	private static int[] keyIndices;
	private ArrayList<T> record;
	
	/**
	 * Default constructor, instantiates an empty DataRecord object.
	 */
	@SuppressWarnings("unchecked")
	public DataRecordW()
	{
		record = (ArrayList<T>) new ArrayList<String>();
	}
	
	/**
	 * Constructor to hold a single record from a database.
	 * @param record The data for the record of interest.
	 * @param compareOn Determines how the compareTo method functions, 0 compares with all keys, 1 uses only the
	 *   first, 2 uses only the second, 3 uses only the third. 
	 * @param keys The index values of the key fields. Enter between one to three keys separated by commas. The
	 *   sort priority corresponds to the order entered (1st is more important than 2nd).
	 * @throws IllegalArgumentException Thrown when DataRecord.setRecord(), DataRecord.setCompareOn() or 
	 *   DataRecord.setKeyIndices() would throw an exception.   
	 */
	public DataRecordW(T[] record, int compareOn, int...keyIndices) throws IllegalArgumentException
	{
		if (record.length >= keyIndices.length)
		{	this.record = new ArrayList<T>(record.length);
			for (T field:record)
				this.record.add(field);
		}
		else throw new IllegalArgumentException("There are too few values in \'record\' for the number of \'keyIndices\'");
		setKeyIndices(keyIndices);	// reuses code by calling the set method
		setCompareOn(compareOn);	// reuses code by calling the set method
	}
	
	/**
	 * Creates a DataRecord from the given generic array.
	 * @param record The array of data type T to be converted to a DataRecord.
	 * @throws IllegalArgumentException If the length of the array is shorter than the number of values to be compared.
	 */
	public DataRecordW(T[] record) throws IllegalArgumentException
	{
		if (record.length >= keyIndices.length)
		{	this.record = new ArrayList<T>(record.length);
			for (T field:record)
				this.record.add(field);
		}
		else throw new IllegalArgumentException("There are too few values in \'record\' for the number of \'keyIndices\'");
	}
	
	/**
	 * Creates a DataRecord from the given generic ArrayList.
	 * @param record The array of data type T to be converted to a DataRecord.
	 * @throws IllegalArgumentException If the length of the array is shorter than the number of values to be compared.
	 */
	public DataRecordW(ArrayList<T> record, int compareOn, int[] keyIndices) throws IllegalArgumentException
	{
		if (record.size() >= keyIndices.length)
		{	DataRecordW.compareOn = compareOn;
			int i;
			DataRecordW.keyIndices = new int[keyIndices.length];
			for(i = 0; i < keyIndices.length; i++)
			{	DataRecordW.keyIndices[i] = keyIndices[i];
			}
			this.record = new ArrayList<T>();
			for (T field:record)
				this.record.add(field);
		}
		else throw new IllegalArgumentException("There are too few values in \'record\' for the number of \'keyIndices\'");
	}
	
	/**
	 * Returns the 'record' held by the DataRecord object, a representation of a single record from a database.
	 *  The record can have multiple fields, as such it is stored as an ArrayList.
	 * @return the record of interest.
	 */
	public ArrayList<T> getRecord() {
		return record;
	}


	/**
	 * Use this method to set the 'record' property to be a representation of a single record from a database table.
	 *  This method overwrites any data stored in the DataRecord's 'record' property.
	 * @param record The data from one record in a database, each field should be one cell in an ArrayList of generic type T.
	 * @throws IllegalArgumentException Thrown when 'record' has fewer fields than the number of 'keyIndices'
	 */
	public void setRecord(T[] record)  throws IllegalArgumentException
	{
		if (record.length >= DataRecordW.keyIndices.length)
		{	this.record = new ArrayList<T>(record.length);
			for (T field:record)
				this.record.add(field);
		}
		else throw new IllegalArgumentException("There are too few values in \'record\' for the number of \'keyIndices\'");

	}


	/**
	 * Returns how the compareTo method compares objects. 
	 * @return When 0 all indices are used, the order of priority is set by the order in the 'keyIndices' array.
	 *    When 1, 2, or 3 only the 1st, 2nd, or 3rd key field, respectively, is used for comparison.
	 */
	public static int getCompareOn() {
		return compareOn;
	}


	/**
	 * Sets how the compareTo method compares objects.
	 * @param compareOn When 0 all indices are used, the order of priority is set by the order in 'keyIndices'.
	 *    When 1, 2, or 3 only the 1st, 2nd, or 3rd key field, respectively, is used for comparison.
	 * @throws IllegalArgumentException Thrown when the value of compareOn is negative or greater than the
	 *  number of key fields.
	 */
	public static void setCompareOn(int compareOn) throws IllegalArgumentException
	{
		if (compareOn >= 0 && compareOn <= keyIndices.length)
			DataRecordW.compareOn = compareOn;
		else throw new IllegalArgumentException("Values for \'compareOn\' and \'keyIndices\' do not agree");
	}


	/**
	 * Returns the key fields that are used by compareTo(). The highest sort priority is given to the first key [0],
	 *  and the lowest to the last.  Priority is only relevant if the 'compareOn' property is set to 0. 
	 * @return The array of indices.  An array of the indices of the key fields. They indices correspond to the 
	 *  fields in 'record'.  
	 */
	public static int[] getKeyIndices() {
		return keyIndices;
	}

	/**
	 * Set a single value of keyIndices, note be very careful and be sure to set it back!!!
	 * @param index The position in the keyIndices array.
	 * @param value The value to set at the 'index' position. 
	 */
	public static void setKeyIndex(int index, int value)
	{	DataRecordW.keyIndices[index] = value;
	}

	/**
	 * Sets the key fields to be used by compareTo().  The highest sort priority is given to the first key [0],
	 *  and the lowest to the last.  Priority is only relevant if the 'compareOn' property is set to 0.
	 * @param keyIndices A comma separated list of the indices of fields in 'record' that should be used as key 
	 *  fields for the compareTo() method.
	 * @throws IllegalArgumentException Thrown when either an index does not refer to a field in 'record' or the
	 *  number of indices is not appropriate, e.g. less than 1 or greater than the number of fields in 'record'. 
	 */
	public void setKeyIndices(int...keyIndices) throws IllegalArgumentException
	{
		int numKeys = keyIndices.length;
		if (numKeys > 0 && numKeys <= 3)
		{	boolean indicesValid = true;
			for (int index:keyIndices)
				if (index >= record.size() || index < 0)
					indicesValid = false;
			if (indicesValid)
			{	DataRecordW.keyIndices = new int[numKeys];
				System.arraycopy(keyIndices, 0, DataRecordW.keyIndices, 0, numKeys);
			}
			else new IllegalArgumentException("A value in the list of \'keyIndices\' is invalid");
		}
		else throw new IllegalArgumentException("Invalid number of \'keyIndices\'");
		
	}

	/**
	 * This method returns the values used by the 'compareTo' method for a given 'compareOn'.
	 * @param compareOn When 0 the method returns all comparison values, 1 returns only the first priority value, 
	 *   2 only the second, 3 only the third. 
	 * @return The values used for comparison, ranked in order of descending priority (highest priority is at index 0).
	 */
	@SuppressWarnings("unchecked")
	public Comparable<T>[] getComparisonValues(int compareOn)
	{
		Comparable<T>[] values;
		switch (compareOn)
		{
			case 0:
				if (keyIndices.length == 1)
				{	values = (Comparable<T>[]) Array.newInstance(Comparable.class, 1);
					values[0] = record.get(keyIndices[0]);
				}
				else if (keyIndices.length == 2)
				{	values = (Comparable<T>[]) Array.newInstance(Comparable.class, 2);
					values[0] = record.get(keyIndices[0]);
					values[1] = record.get(keyIndices[1]);
				}
				else if (keyIndices.length == 3)
				{	values = (Comparable<T>[]) Array.newInstance(Comparable.class, 3);
					values[0] = record.get(keyIndices[0]);
					values[1] = record.get(keyIndices[1]);
					values[2] = record.get(keyIndices[2]);
				}
				else throw new IllegalArgumentException("Length of keyIndices: " + keyIndices.length +" is invalid.");
				break;
			case 1:
				values = (Comparable<T>[]) Array.newInstance(Comparable.class, 1);
				values[0] = record.get(keyIndices[0]);
				break;
			case 2:
				values = (Comparable<T>[]) Array.newInstance(Comparable.class, 1);
				values[0] =  record.get(keyIndices[1]);
				break;
			case 3:
				values = (Comparable<T>[]) Array.newInstance(Comparable.class, 1);
				values[0] =  record.get(keyIndices[2]);
				break;
			default: throw new IllegalArgumentException("Illegal 'compareOn' value: " + compareOn + ".");
		}
		return values;
	}

	/**
	 * The method relies on the 'compareOn' property. When 0 all indices are used, the order of priority is set 
	 *   by the order in 'keyIndices'. When 1, 2, or 3 only the 1st, 2nd, or 3rd key field, respectively, is used. 
	 * @param otherRecord The record that the calling object is to be compared to. Generally, returns a positive value when the calling
	 *  object is greater than, a negative value when less than, and 0 when equal. See specific details for the
	 *  compareTo method of the underlying data type T.
	 * @throws IllegalArgumentException Thrown when T does not implement Comparable, or 'compareOn' does not have
	 *  a valid value.
	 * @Override compareTo 
	 */
	public int compareTo(DataRecordW<T> otherRecord) throws IllegalArgumentException
	{
		if (otherRecord instanceof Comparable)
		{
			int result = 0;
			switch (compareOn)
			{
				case 0:											// Compare all index fields, in order of priority.
					result = getComparison(otherRecord, 1);
					if (result == 0 && keyIndices.length > 1)	// If 1st priority is equal, check 2nd priority
						result = getComparison(otherRecord, 2);
					if (result == 0 && keyIndices.length > 2)	// 		if 2nd is also equal, check 3rd
						result = getComparison(otherRecord, 3);
					break;
				case 1:											// Compare only field1
					result = getComparison(otherRecord, 1);
					break;
				case 2:											// Compare only field2
					result = getComparison(otherRecord, 2);
					break;
				case 3:											// Compare only field3
					result = getComparison(otherRecord, 3);
					break;	
				default:
					throw new IllegalArgumentException("Illegal value for 'compareOn' property.");
			}		
			return result;
		}
		else throw new IllegalArgumentException("Data Type of \'otherRecord\' does not implement Comparable.");
	}

	/**
	 * Helper method for 'compareTo' method.  This method performs the specific comparison requested.
	 * @param otherRecord The record that the calling object is being compared to.
	 * @param compareOnSpecific Set to 1 for the primary, 2 for the secondary or 3 for the tertiary comparison.
	 *   An exception will be thrown if set to any value besides these.  
	 * @return The result of the specific 'compareTo' comparison for type T.
	 * @throws IllegalArgumentException Is thrown if 'compareOnSpecific' is not 1, 2 or 3.  It will also throw
	 *   if an invalid comparison is requested (e.g. tertiary when only 2 keys are established).
	 */
	private int getComparison(DataRecordW<T> otherRecord, int compareOnSpecific) throws IllegalArgumentException
	{
		if (compareOnSpecific > 0 && compareOnSpecific <= keyIndices.length)
		{	int index = keyIndices[compareOnSpecific - 1];
			int result = 0;
			if (this.record.get(index) == null || otherRecord.getRecord().get(index) == null)	// cases where one or both are null
			{
				if (this.record.get(index) == null)		
					result = -1;
				if (otherRecord.getRecord().get(index) == null)
					result = 1;
				if (this.record.get(index) == null && otherRecord.getRecord().get(index) == null)
					result = 0;
			}
			else
				result = this.record.get(index).compareTo(otherRecord.getRecord().get(index));	// use base data type's comparison
			return result;
		}
		else throw new IllegalArgumentException("Value of 'compareOnSpecific', " + compareOnSpecific + ", does not exist.");
	}
	
	public DataRecordW<T> clone()
	{	
		ArrayList<T> list = new ArrayList<T>(); 
		int i;
		for(i = 0; i < record.size(); i++)
			list.add(record.get(i));
		return new DataRecordW<T>(list, DataRecordW.compareOn, DataRecordW.keyIndices);
	}
	
	public int size()
	{	return record.size();
	}
	
	public T get(int index)
	{	return record.get(index);
	}
	
	public boolean add(T element)
	{	return record.add(element);
	}
	
	/**
	 * This is a shallow equals method. It uses the compareTo method.
	 * @param otherRecord Should be another DataRecordW<T>.
	 * @return True if compareTo == 0, false if not or not comparable.
	 */
	@SuppressWarnings("unchecked")
	public boolean equals(Object otherRecord)
	{	boolean result = false;
		if (otherRecord instanceof DataRecordW<?>)
		{	try
			{	if(this.compareTo((DataRecordW<T>) otherRecord) == 0)
					result =  true;
			}
			catch(Exception e)
			{// do nothing
			}
		}
		return result;
	}
	
	/**
	 * 
	 * @return An iterator over the elements of the 'record'.
	 */
	public Iterator<T> iterator()
	{	return record.iterator();
	}
	
	/**
	 * The toString method for a DataRecord, a tab separated list of the data.
	 */
	@Override
	public String toString()
	{
		String message = "";
		for (int cell = 0; cell < record.size(); cell++)
			message += record.get(cell) + "\t";
		return message;
	}
	
	/**
	 * Demonstrates the clone method for nested DataRecordW objects.
	 * @param args
	 */
	public static void main(String[] args)
	{	String[] fields = new String[] {"a","b","c","1","2","3"};
		String[] others = new String[] {"3","b","2","a","1","c"};
		int[] keys = new int[]{0,1,2};
		DataRecordW<String> one = new DataRecordW<String>(fields, 0, keys);
		DataRecordW<String> two = one.clone();
		System.out.println(one + "\n" + two);
		DataRecordW<String> three = new DataRecordW<String>(others, 0, keys);
		ArrayList<DataRecordW<String>> list = new ArrayList<DataRecordW<String>>();
		list.add(one);
		list.add(three);
		list.add(two);
		DataRecordW<DataRecordW<String>> doub = new DataRecordW<DataRecordW<String>>(list, 0, keys);
		DataRecordW<DataRecordW<String>> copy2 = doub.clone();
		System.out.println("\n"+doub+"\n"+copy2);
	}
}