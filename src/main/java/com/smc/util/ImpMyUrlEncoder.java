/*
 * MyUrlEncoder.java
 *
 * Created on 2002/12/15, 5:54
 */

package com.smc.util;


/**
 *
 * @author  miyasit
 * @version 
 */
public class ImpMyUrlEncoder {

    static final String NOTESCAPE 
    = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789.-*_";

  /** Creates new MyUrlEncoder */
    public ImpMyUrlEncoder() {
    }
    

    public static String encode(String str, String enc) {
    	
    	StringBuffer buf = new StringBuffer();
        
    	try{
    		
	    	byte[] bytes = str.getBytes(enc);
	        for (int i=0; i<bytes.length; i++) {
	          char c = (char) (bytes[i]>0 ? bytes[i] : bytes[i] + 256);
	          if ( c == ' ' && false ) { // false �� JavaScript �� unescape �Ή�
	            buf.append("+"); 
	          } else if ( NOTESCAPE.indexOf(c) >= 0 ) {
	            buf.append(c);
	          } else {
	            buf.append("%" + Integer.toHexString(c) );
	          }
	        }
	        
    	}catch(Exception ex){
    		ex.printStackTrace();
    	}
        
        return buf.toString();
    }

    
    
    public static String decode(String str, String enc)
        throws java.io.UnsupportedEncodingException {

        StringBuffer buf = new StringBuffer();
        for (int i=0; i<str.length(); i++) {
          char c = str.charAt(i);
          if ( c == '+' && false ) { // false �� JavaScript �� escape �Ή�
            buf.append(" ");
          } else if ( c == '%' ) {
            c = (char) Integer.parseInt(str.substring(i+1, i+3), 16);
            if ( "/%.\\\0".indexOf(c) >= 0 ) {
              buf.append(buf.append(str.substring(i, i+3)));
            } else {
              buf.append(c) ;
            }
            i += 2;
          } else {
            buf.append(c);
          }
        }
    return new String( buf.toString().getBytes("8859_1"), enc);
    }
    
    
    
}
