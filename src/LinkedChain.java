/**
 * A generic bag of entries stored in singly linked nodes.
 * No order is promised to callers, no nulls are stored, and duplicates
 * are allowed. This class holds no application logic.
 *
 * @param <T> the type of entry stored
 *
 * @author Oscar
 * @version 2026.10.15
 */
public class LinkedChain<T> {

    private Node<T> firstNode;
    private int numberOfEntries;

    /**
     * One link in the chain. Static with its own type parameter because
     * a static nested class cannot see the outer class's T.
     *
     * @param <E> the data type
     */
    private static class Node<E> {
        private E data;
        private Node<E> next;

        /**
         * @param data the entry to hold
         */
        Node(E data) {
            this.data = data;
        }
    }

    /**
     * Creates an empty chain.
     */
    public LinkedChain() {
        firstNode = null;
        numberOfEntries = 0;
    }

    /**
     * Inserts at the front. O(1).
     *
     * @param newEntry the entry to add
     * @return true
     * @throws IllegalArgumentException if newEntry is null
     */
    public boolean add(T newEntry) {
        if (newEntry == null) {
            throw new IllegalArgumentException("Cannot add null");
        }
        Node<T> newNode = new Node<T>(newEntry);
        newNode.next = firstNode;
        firstNode = newNode;
        numberOfEntries++;
        return true;
    }

    /**
     * Inserts at the back. O(n).
     *
     * @param newEntry the entry to add
     * @return true
     * @throws IllegalArgumentException if newEntry is null
     */
    public boolean addToEnd(T newEntry) {
        if (newEntry == null) {
            throw new IllegalArgumentException("Cannot add null");
        }
        Node<T> newNode = new Node<T>(newEntry);
        if (firstNode == null) {
            firstNode = newNode;
        }
        else {
            Node<T> current = firstNode;
            while (current.next != null) {
                current = current.next;
            }
            current.next = newNode;
        }
        numberOfEntries++;
        return true;
    }

    /**
     * Removes and returns the front entry. O(1).
     *
     * @return the removed entry, or null if the chain is empty
     */
    public T remove() {
        if (firstNode == null) {
            return null;
        }
        T result = firstNode.data;
        firstNode = firstNode.next;
        numberOfEntries--;
        return result;
    }

    /**
     * Unlinks the first entry equal to anEntry. O(n).
     *
     * @param anEntry the entry to remove
     * @return true if an entry was removed; false if absent or null
     */
    public boolean remove(T anEntry) {
        if (anEntry == null) {
            return false;
        }
        Node<T> previous = null;
        Node<T> current = firstNode;
        while (current != null) {
            if (current.data.equals(anEntry)) {
                if (previous == null) {
                    firstNode = current.next;
                }
                else {
                    previous.next = current.next;
                }
                numberOfEntries--;
                return true;
            }
            previous = current;
            current = current.next;
        }
        return false;
    }

    /**
     * Drops every entry. O(1).
     */
    public void clear() {
        firstNode = null;
        numberOfEntries = 0;
    }

    /**
     * Returns the entry at a position, front = 0. O(n).
     *
     * @param index position from the front
     * @return the entry there
     * @throws IndexOutOfBoundsException if index is out of range
     */
    public T get(int index) {
        if (index < 0 || index >= numberOfEntries) {
            throw new IndexOutOfBoundsException("Index " + index
                + " out of range for size " + numberOfEntries);
        }
        Node<T> current = firstNode;
        for (int i = 0; i < index; i++) {
            current = current.next;
        }
        return current.data;
    }

    /**
     * @param anEntry the entry to look for
     * @return true if at least one equal entry is stored
     */
    public boolean contains(T anEntry) {
        return count(anEntry) > 0;
    }

    /**
     * Counts entries equal to anEntry. O(n).
     *
     * @param anEntry the entry to count
     * @return how many equal entries; 0 for null
     */
    public int count(T anEntry) {
        if (anEntry == null) {
            return 0;
        }
        int count = 0;
        Node<T> current = firstNode;
        while (current != null) {
            if (current.data.equals(anEntry)) {
                count++;
            }
            current = current.next;
        }
        return count;
    }

    /**
     * @return the number of entries
     */
    public int size() {
        return numberOfEntries;
    }

    /**
     * @return true if there are no entries
     */
    public boolean isEmpty() {
        return numberOfEntries == 0;
    }

    /**
     * Copies the entries, front to back, into the caller's array.
     * The caller creates the array (e.g. new Memory[chain.size()]) so
     * the chain never needs an unchecked generic-array cast. O(n).
     *
     * @param values an array at least size() long
     * @return the same array, filled
     * @throws IllegalArgumentException if values is null or too small
     */
    public T[] toArray(T[] values) {
        if (values == null || values.length < numberOfEntries) {
            throw new IllegalArgumentException(
                "Array must be non-null and at least size " + numberOfEntries);
        }
        int i = 0;
        Node<T> current = firstNode;
        while (current != null) {
            values[i] = current.data;
            i++;
            current = current.next;
        }
        return values;
    }

    /**
     * @return entries front to back, like [a, b, c]
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("[");
        Node<T> current = firstNode;
        while (current != null) {
            sb.append(current.data);
            if (current.next != null) {
                sb.append(", ");
            }
            current = current.next;
        }
        sb.append("]");
        return sb.toString();
    }
}
