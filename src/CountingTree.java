import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

/**
 * This class acts as a TreeSet that counts the number of times a key is added to the set. The count for a key is accessed 
 *   with the getCount(key) method. The decrement(key) method reduces that count by one. The iteratorWCounts()
 *   method returns an iterator of all the keys and their counts. The remaining methods override methods in the Set, Iterable
 *   and Collection interfaces. The removeAll, retainAll and toArray methods are not implemented. The main method demonstrates
 *   the implemented methods.
 * @author thayer young, thayer.young@cicadagis.com
 * @version 1.0 12/21/2015
 */
public class CountingTree implements Set<Object>, Iterable<Object>, Collection<Object>
{
	private TreeMap<Object,Integer> tree;
	
	/**
	 * Constructor
	 */
	public CountingTree()
	{	tree = new TreeMap<Object,Integer>();
	}
	
	/**
	 * Adds a key to the tree. If the key is in the tree, the count is incremented. Exception safe.
	 * @param The key to be added
	 * @returns True if the key was comparable and therefore able to be added to or already exists in the tree. 
	 *    False if it was not comparable, and therefore the tree is unchanged.
	 * @Override
	 */
	public boolean add(Object key)
	{	int count;
		boolean result;
		if(this.contains(key))
		{	count = tree.get(key).intValue() + 1;
			tree.put(key, Integer.valueOf(count));
			result = true;
		}
		else
		{	count = 1;
			try
			{	tree.put(key, count);
				result = true;
			}
			catch(Exception e)
			{	result = false;
			}
		}
		return result;
	}
	
	/**
	 * Reduces the count for the key by one, removes the key from the tree if the count falls below 1. Exception safe.
	 * @param key The key to be decremented.
	 * @return The count for the key after decrementing. A count of 0 is returned if the key is not present or removed.
	 */
	public int decrement(Object key)
	{	int count = 0;
		if (this.contains(key))
		{	count = tree.get(key).intValue() - 1;
			if(count < 1)
				tree.remove(key);
			else tree.put(key, Integer.valueOf(count));
		}
		return count;
	}
	
	/**
	 * Deletes the key from the tree regardless of its count. Exception safe.
	 * @param key The key to be deleted from the tree.
	 * @return true if key was in the tree before it was deleted. False if it was not in the tree.
	 * @Override
	 */
	public boolean remove(Object key)
	{	boolean result = false;
		if (this.contains(key))
		{	tree.remove(key);
			result = true;
		}
		return result;
	}
	
	/**
	 * Gets the count for the key. Exception safe.
	 * @param key The key for which the count is desired
	 * @return The count of times the key has been added to the tree, or 0 if not present in the tree.
	 */
	public int getCount(Object key)
	{	int count = 0;
		if(this.contains(key))
			count = tree.get(key).intValue();
		return count;
	}
	
	/**
	 * Returns the number of keys in the tree.
	 * @Override
	 */
	public int size()
	{	return tree.size();
	}

	/**
	 * Empties all keys and their counts from the tree.
	 * @Override
	 */
	public void clear() {
		tree.clear();
	}

	/**
	 * Searches the tree for the desired key. Exception safe.
	 * @param key The key to be found
	 * @return True if the tree contains the key, false if the key is not found or is not comparable.
	 * @Override
	 */
	public boolean contains(Object key) 
	{	boolean result = false;
		try
		{	if(tree.containsKey(key))
				result = true;
		}
		catch(Exception e)
		{	result = false;
//System.out.println("\t\t.contains exception for key: " + key + "\n\t\t\t" + e);
//e.printStackTrace();
		}
		return result;
	}

	/**
	 * @return True if there are no keys in the tree.
	 * @Override
	 */
	public boolean isEmpty() {
		if(tree.isEmpty())
			return true;
		return false;
	}

	/**
	 * Returns an iterator of the keys and their counts.
	 * @return The desired iterator.
	 */
	public Iterator<Entry<Object,Integer>> iteratorWCounts()
	{	return tree.entrySet().iterator();
	}
	
	/**
	 * Returns an iterator of just the keys. Use iteratorWCounts() to get an iterator of both keys and their counts
	 * @return A set of the keys of the map
	 * @Override 
	 */
	public Iterator<Object> iterator() 
	{	return tree.keySet().iterator();
	}
	
	/**
	 * Returns the hash code for the underlying TreeMap
	 * @return The hash code for the TreeMap
	 * @Override 
	 */
	public int hashCode()
	{	return tree.hashCode();
	}
	
	/**
	 * Adds the keys in the supplied collection. None are added if any of the keys fail to add.
	 * @param keys The collection of keys to add.
	 * @return False if any of the keys fail to add.
	 * @Override
	 */
	public boolean addAll(Collection<? extends Object> keys) 
	{	Iterator<?> iter;
		boolean result = true;
		boolean curRes;
		CountingTree check = this.clone();
		if(keys.size() > 0)
		{	iter = keys.iterator();
			while(iter.hasNext())
			{	curRes = check.add(iter.next());
				if(curRes == false)
					result = false;
			}
			if (result == true)
				this.tree = check.tree;
		}
		return result;
	}
	
	/**
	 * This method returns a safe copy of the original object. Test to be sure sub-objects are correctly copied.
	 * @return A clone of the CountingTree on which it is called.
	 * @Override
	 */
	public CountingTree clone()
	{	CountingTree copy = new CountingTree();
		Set<Object> set = tree.keySet();
		Iterator<Object> iter = set.iterator();
		int count;
		while (iter.hasNext())
		{	Object cur = iter.next();
			if(this.contains(cur))
			{	count = this.getCount(cur);
				for(int i = 0;i<count;i++)
					copy.add(cur);
			}
		}
		return copy;
	}
	
	/**
	 * Checks if 'keys' is a subset of the tree.
	 * @param The collection of keys to be checked.
	 * @return True if all of the keys in 'keys' are found in the tree. The count is irrelevant.
	 * @Override
	 */
	public boolean containsAll(Collection<?> keys) 
	{	Iterator<?> iter;
		boolean matches = true;
		if(keys.size() > 0)
		{	iter = keys.iterator();
			while(iter.hasNext())
			{	if(!this.contains(iter.next()))
					matches = false;
			}
		}
		return matches;
	}

	/**
	 * @return A tab separated table of tree entries with the count followed by the key
	 * @Override
	 */
	public String toString()
	{	String result = "";
		CountingTree copy = this.clone();
		Iterator<Entry<Object, Integer>> iter = copy.iteratorWCounts();
		while(iter.hasNext())
		{	Entry<Object, Integer> cur = iter.next();
			result += cur.getValue() + "\t" + cur.getKey() + "\n";			
		}
		return result;
	}
	
	/**
	 * This method is not implemented.
	 * @Override
	 */
	public boolean removeAll(Collection<?> c) {
		return false;
	}

	/**
	 * This method is not implemented.
	 * @Override
	 */
	public boolean retainAll(Collection<?> c) {
		return false;
	}

	/**
	 * This method is not implemented.
	 * @Override
	 */
	public Object[] toArray() {
		return null;
	}

	/**
	 * This method is not implemented.
	 * @Override
	 */
	public <T> T[] toArray(T[] a) {
		return null;	
	}	
	
	/**
	 * A demonstration of the class and its methods.
	 * @param args
	 */
	public static void main(String[] args)
	{	CountingTree counter = new CountingTree(); 
		Object int5 = new Integer(5);
		Object int8 = new Integer(8);
		Object str5 = new String("5");
		Object strBob = new String("Bob");
		Object toHello = new TestObject("Hello");
		
		System.out.println("Demonstration of the CountingTree class, written by Thayer Young\n\n"
		 + "1st demo, after each operation getCount(key) is called and the count is printed before the operation, the key and the size of the tree.\n"
		 + "The other demonstrated methods are: add and decrement. Also shown is that a tree can only take one type of Object:\n4x add Integer(5), 1x decrement Integer(5), 2x add TestObject(\"Hello\"), 1x add \"Bob\", 2x add Integer(8) 1x decrement"); 
		counter.add(int5);			System.out.println("Integer " + int5 + "\tcount: "+counter.getCount(int5)+ "\tafter:\tadd\tsize: " + counter.size());
		counter.add(int5);			System.out.println("Integer " + int5 + "\tcount: "+counter.getCount(int5)+ "\tafter:\tadd\tsize: " + counter.size());
		counter.add(int5);			System.out.println("Integer " + int5 + "\tcount: "+counter.getCount(int5)+ "\tafter:\tadd\tsize: " + counter.size());
		counter.decrement(int5);	System.out.println("Integer " + int5 + "\tcount: "+counter.getCount(int5)+ "\tafter:\tdecrement size: " + counter.size());
		counter.add(int5);			System.out.println("Integer " + int5 + "\tcount: "+counter.getCount(int5)+ "\tafter:\tadd\tsize: " + counter.size());
		counter.add(toHello);		System.out.println("TestObject " + toHello + " count: "+counter.getCount(toHello)+ "\tafter:\tadd\tsize: " + counter.size());
		counter.add(toHello);		System.out.println("TestObject " + toHello + " count: "+counter.getCount(toHello)+ "\tafter:\tadd\tsize: " + counter.size());
		counter.add(strBob);		System.out.println("Sting " + strBob + "\tcount: "+counter.getCount(strBob)+ "\tafter:\tadd\tsize: " + counter.size());
		counter.add(int8);			System.out.println("Integer " + int8 + "\tcount: "+counter.getCount(int8)+ "\tafter:\tadd\tsize: " + counter.size());
		counter.add(int8);			System.out.println("Integer " + int8 + "\tcount: "+counter.getCount(int8)+ "\tafter:\tadd\tsize: " + counter.size());
		counter.decrement(int8);	System.out.println("Integer " + int8 + "\tcount: "+counter.getCount(int8)+ "\tafter:\tdecrement size: " + counter.size());
		System.out.println("\nFinal Results (count  key): The following demonstrates the toString method:\n"+counter.toString());
		
		counter.clear();
		System.out.println("Demonstrate what is left in the tree after .clear():"+counter.toString()+"\n\n");
		
		System.out.println("2nd demo, same demo with Strings:\n4x add String(\"5\"), 1x decrement String(\"5\"), 1x decrement Integer(5), 2x add TestObject(\"Hello\"), 1x add \"Bob\", 2x add Integer(8)");
		counter.add(str5);
		counter.add(str5);
		counter.add(str5);
		counter.decrement(str5);
		counter.decrement(int5);
		counter.add(str5);
		counter.add(toHello);
		counter.add(toHello);
		counter.add(strBob);
		counter.add(int8);
		counter.add(int8);
		System.out.println("\nFinal Results (count  key):\n"+counter.toString());
		
		counter.clear();
		System.out.println("after .clear():"+counter.toString()+"\n\n");

		System.out.println("3rd demo, nothing is added if tree initialized with a non Comparable object:\n2x add TestObject(\"Hello\"), 4x add String(\"5\"), 1x decrement String(\"5\"), 1x decrement Integer(5), , 1x add \"Bob\", 2x add Integer(8)");
		counter.add(toHello);
		counter.add(toHello);
		counter.add(str5);
		counter.add(str5);
		counter.add(str5);
		counter.decrement(str5);
		counter.add(str5);
		counter.add(strBob);
		counter.add(int8);
		counter.add(int8);
		counter.decrement(int8);
		System.out.println("\nFinal Results (count  key):\n"+counter.toString());	
		
		counter.clear();
		System.out.println("after .clear():"+counter.toString()+"\n\n");
		
		System.out.println("4th demo, Demonstrate remove:\n3x add Integer(5), 1x remove String(\"5\"), 3x add Integer(8), 1x remove Integer(8)"); 
		counter.add(int5);
		counter.add(int5);
		counter.add(int5);
		counter.remove(str5);
		counter.add(int8);
		counter.add(int8);
		counter.add(int8);
		counter.remove(int8);
		System.out.println("\nFinal Results (count  key):\n"+counter.toString());
		
		LinkedList<Object> list = new LinkedList<Object>();
		list.add(int5);
		list.add(int5);
		System.out.println("Without clearing the tree:\n"+
		                   "Demonstrate the containsAll method for a LinkedList with 2 entries of Integer 5:\t"
				+ counter.containsAll(list));
		list.add(int8);
		System.out.println("Demonstrate the containsAll method for the same LinkedList also with Integer 8: \t"
				+ counter.containsAll(list));
		System.out.println("Use that LinkedList to demonstrate the addAll method:                           \t"
				+ counter.addAll(list) + "\n" + counter.toString());
		System.out.println("Use that LinkedList again to demonstrate the addAll method:                     \t"
				+ counter.addAll(list) + "\n" + counter.toString());
		list.add("5");
		System.out.println("Add a String \"5\" to the LinkedList and try containsAll again:                   \t"
				+ counter.containsAll(list));
		System.out.println("Try to addAll the LinkedList:                                                     \t"
				+ counter.addAll(list) + "\n" + counter.toString());
		System.out.println("Note that nothing was added and the return is false, because the String is the wrong data type"
				+ "\nNote that the contains, iteratorWCounts and clone methods are all demonstrated within the above demoed methods");
	}
}