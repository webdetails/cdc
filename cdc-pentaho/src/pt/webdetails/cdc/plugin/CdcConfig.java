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


package pt.webdetails.cdc.plugin;

public class CdcConfig extends GenericCdcConfig {

  private static CdcConfig instance;

  public static String getCdaConfigFile() {
    return "plugin.xml";
  }

  public static CdcConfig getConfig() {
    if ( instance == null ) {
      instance = new CdcConfig();
    }
    return instance;
  }
}
