package com.datastax.oss.cass_stac;

import com.datastax.oss.cass_stac.dao.DaoFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class App
{
    private static final Logger logger = LoggerFactory.getLogger(App.class);

    public static void main( String[] args ) throws Exception
    {
      DaoFactory df = DaoFactory.getInstance();
      logger.info("Hello World!");
      System.exit(0);
    }
}
