package org.example;

import com.uber.h3core.H3Core;

/**
 * Hello world!
 *
 */
public class App 
{
    public static void main( String[] args ) throws Exception
    {
      H3Core h3 = H3Core.newInstance();
      double lat = 37.7955;
      double lng = -122.3937;
      int res = 10;

      long cell = h3.latLngToCell(lat, lng, res);
      String hexString = String.format("%016x", cell);

      System.out.println("Hello World! " + hexString);

    }
}
