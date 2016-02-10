package com.insightde.utils;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import com.google.common.base.Functions;
import com.google.common.collect.Ordering;

public class ValueComparableMap <K extends Comparable<K>,V> extends TreeMap<K,V>{
	//A map for doing lookups on the keys for comparison so we don't get infinite loops
    private final Map<K, V> valueMap;

    public ValueComparableMap(final Ordering<? super V> partialValueOrdering) {
        this(partialValueOrdering, new HashMap<K,V>());
    }

    private ValueComparableMap(Ordering<? super V> partialValueOrdering,
            HashMap<K, V> valueMap) {
        super(partialValueOrdering //Apply the value ordering
                .onResultOf(Functions.forMap(valueMap)) //On the result of getting the value for the key from the map
                .compound(Ordering.natural())); //as well as ensuring that the keys don't get clobbered
        this.valueMap = valueMap;
    }

    public V put(K k, V v) {
        if (valueMap.containsKey(k)){
            //remove the key in the sorted set before adding the key again
            remove(k);
        }
        valueMap.put(k,v); //To get "real" unsorted values for the comparator
        return super.put(k, v); //Put it in value order
    }

    public static void main(String[] args){
        TreeMap<String, Double> map = new ValueComparableMap<String, Double>(Ordering.natural());
        map.put("a", (double) 5);
        map.put("b", (double) 1);
        map.put("c", (double) 3);
        assertEquals("b",map.firstKey());
        assertEquals("a",map.lastKey());
        map.put("d",(double) 0);
        assertEquals("d",map.firstKey());
        //ensure it's still a map (by overwriting a key, but with a new value) 
        map.put("d", (double) 2);
        assertEquals("b", map.firstKey());
        //Ensure multiple values do not clobber keys
        map.put("e", (double) 2);
//        assertEquals(5, map.size());
//        assertEquals(2, (int) map.get("e"));
//        assertEquals(2, (int) map.get("d"));
    }
}
