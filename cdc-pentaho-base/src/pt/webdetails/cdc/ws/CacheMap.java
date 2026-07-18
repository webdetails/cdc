/*! ******************************************************************************
 *
 * Pentaho
 *
 * Copyright (C) 2002 - 2026 by Pentaho Canada Inc. : http://www.pentaho.com
 *
 * Use of this software is governed by the Business Source License included
 * in the LICENSE.TXT file.
 *
 * Change Date: 2030-06-15
 ******************************************************************************/


package pt.webdetails.cdc.ws;

import pt.webdetails.cdc.plugin.CdcConfig;

import java.util.HashMap;
import java.util.Map;

public enum CacheMap {

  Cda( CdcConfig.CacheMaps.CDA_MAP ),
  Mondrian( CdcConfig.CacheMaps.MONDRIAN_MAP ),;

  private static final Map<String, CacheMap> mapNameResolver = new HashMap<String, CacheMap>();
  private String name;

  static {
    mapNameResolver.put( "cdaCache", CacheMap.Cda );
    mapNameResolver.put( "cda", CacheMap.Cda );
    mapNameResolver.put( "cdacache", CacheMap.Cda );
    mapNameResolver.put( "CDA", CacheMap.Cda );

    mapNameResolver.put( "mondrian", CacheMap.Mondrian );
    mapNameResolver.put( "mondrianCache", CacheMap.Mondrian );
    mapNameResolver.put( "mondriancache", CacheMap.Mondrian );
    mapNameResolver.put( "MONDRIAN", CacheMap.Mondrian );
  }

  CacheMap( String name ) {
    this.name = name;
  }

  public String getName() {
    return this.name;
  }

  public static CacheMap parse( String textValue ) {
    return mapNameResolver.get( textValue );
  }

}
