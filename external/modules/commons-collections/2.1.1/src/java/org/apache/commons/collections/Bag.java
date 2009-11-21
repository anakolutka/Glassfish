/*
 * Copyright 1999-2004 The Apache Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.commons.collections;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

/**
 * A {@link Collection} that counts the number of times an object appears in
 * the collection.  Suppose you have a Bag that contains <code>{a, a, b,
 * c}</code>.  Calling {@link #getCount(Object)} on <code>a</code> would return
 * 2, while calling {@link #uniqueSet()} would return <code>{a, b, c}</code>.
 *
 * <P><I>Note that this interface violates the {@link Collection} contract.</I> 
 * The behavior specified in many of these methods is <I>not</I> the same
 * as the behavior specified by {@link Collection}.  The noncompliant methods
 * are clearly marked with "(Violation)" in their summary line.  A future
 * version of this class will specify the same behavior as {@link Collection},
 * which unfortunately will break backwards compatibility with this version.
 *
 * @since 2.0
 * @author Chuck Burdick
 **/
public interface Bag extends Collection {
   /**
    * Return the number of occurrences (cardinality) of the given
    * object currently in the bag. If the object does not exist in the
    * bag, return 0.
    **/
   public int getCount(Object o);

   /**
    * <I>(Violation)</I>
    * Add the given object to the bag and keep a count. If the object
    * is already in the {@link #uniqueSet()} then increment its count as
    * reported by {@link #getCount(Object)}. Otherwise add it to the {@link
    * #uniqueSet()} and report its count as 1.<P>
    *
    * Since this method always increases the size of the bag,
    * according to the {@link Collection#add(Object)} contract, it 
    * should always return <Code>true</Code>.  Since it sometimes returns
    * <Code>false</Code>, this method violates the contract.  A future
    * version of this method will comply by always returning <Code>true</Code>.
    *
    * @return <code>true</code> if the object was not already in the
    *         <code>uniqueSet</code>
    * @see #getCount(Object)
    **/
   public boolean add(Object o);

   /**
    * Add <code>i</code> copies of the given object to the bag and
    * keep a count.
    * @return <code>true</code> if the object was not already in the
    *         <code>uniqueSet</code>
    * @see #add(Object)
    * @see #getCount(Object)
    **/
   public boolean add(Object o, int i);

   /**
    * <I>(Violation)</I>
    * Remove all occurrences of the given object from the bag, and do
    * not represent the object in the {@link #uniqueSet()}.
    *
    * <P>According to the {@link Collection#remove(Object)} method,
    * this method should only remove the <I>first</I> occurrence of the
    * given object, not <I>all</I> occurrences.  A future version of this
    * method will comply with the contract by only removing one occurrence
    * of the given object.
    *
    * @see #remove(Object, int)
    * @return <code>true</code> if this call changed the collection
    **/
   public boolean remove(Object o);

   /**
    * Remove the given number of occurrences from the bag. If the bag
    * contains <code>i</code> occurrences or less, the item will be
    * removed from the {@link #uniqueSet()}.
    * @see #getCount(Object)
    * @see #remove(Object)
    * @return <code>true</code> if this call changed the collection
    **/
   public boolean remove(Object o, int i);

   /**
    * The {@link Set} of unique members that represent all members in
    * the bag. Uniqueness constraints are the same as those in {@link
    * Set}.
    **/
   public Set uniqueSet();

   /**
    * Returns the total number of items in the bag across all types.
    **/
   public int size();

   /**
    * <I>(Violation)</I>
    * Returns <code>true</code> if the bag contains all elements in
    * the given collection, respecting cardinality.  That is, if the
    * given collection <code>C</code> contains <code>n</code> copies
    * of a given object, calling {@link #getCount(Object)} on that object must
    * be <code>&gt;= n</code> for all <code>n</code> in <code>C</code>.
    *
    * <P>The {@link Collection#containsAll(Collection)} method specifies
    * that cardinality should <I>not</I> be respected; this method should
    * return true if the bag contains at least one of every object contained
    * in the given collection.  A future version of this method will comply
    * with that contract.
    **/
   public boolean containsAll(Collection c);

   /**
    * <I>(Violation)</I>
    * Remove all elements represented in the given collection,
    * respecting cardinality.  That is, if the given collection
    * <code>C</code> contains <code>n</code> copies of a given object,
    * the bag will have <code>n</code> fewer copies, assuming the bag
    * had at least <code>n</code> copies to begin with.
    *
    * <P>The {@link Collection#removeAll(Collection)} method specifies
    * that cardinality should <I>not</I> be respected; this method should
    * remove <I>all</I> occurrences of every object contained in the 
    * given collection.  A future version of this method will comply
    * with that contract.
    *
    * @return <code>true</code> if this call changed the collection
    **/
   public boolean removeAll(Collection c);

   /**
    * <I>(Violation)</I>
    * Remove any members of the bag that are not in the given
    * collection, respecting cardinality.  That is, if the given
    * collection <code>C</code> contains <code>n</code> copies of a
    * given object and the bag has <code>m &gt; n</code> copies, then
    * delete <code>m - n</code> copies from the bag.  In addition, if
    * <code>e</code> is an object in the bag but
    * <code>!C.contains(e)</code>, then remove <code>e</code> and any
    * of its copies.
    *
    * <P>The {@link Collection#retainAll(Collection)} method specifies
    * that cardinality should <I>not</I> be respected; this method should
    * keep <I>all</I> occurrences of every object contained in the 
    * given collection.  A future version of this method will comply
    * with that contract.
    *
    * @return <code>true</code> if this call changed the collection
    **/
   public boolean retainAll(Collection c);

   /**
    * Returns an {@link Iterator} over the entire set of members,
    * including copies due to cardinality. This iterator is fail-fast
    * and will not tolerate concurrent modifications.
    **/
   public Iterator iterator();
}





