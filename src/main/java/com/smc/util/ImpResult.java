/*
 * Result.java
 *
 * Created on 2004/01/13, 19:31
 */

package com.smc.util;

import java.util.*;

/**
 *
 * @author  miyasit
 * @version
 */
@SuppressWarnings("serial")
public class ImpResult<E>  extends LinkedList<E> implements Cloneable {


    /** Creates new Result */

    public int getSize(){
        return this.size();
    }

    public int getResultsize(){
        return this.size();
    }

}
