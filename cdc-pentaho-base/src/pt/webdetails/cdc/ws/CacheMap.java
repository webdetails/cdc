/*!
* Copyright 2002 - 2014 Webdetails, a Pentaho company.  All rights reserved.
*
* This software was developed by Webdetails and is provided under the terms
* of the Mozilla Public License, Version 2.0, or any later version. You may not use
* this file except in compliance with the license. If you need a copy of the license,
* please go to  http://mozilla.org/MPL/2.0/. The Initial Developer is Webdetails.
*
* Software distributed under the Mozilla Public License is distributed on an "AS IS"
* basis, WITHOUT WARRANTY OF ANY KIND, either express or  implied. Please refer to
* the license for the specific language governing your rights and limitations.
*/

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
