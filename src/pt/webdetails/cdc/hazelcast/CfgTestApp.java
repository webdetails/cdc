/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. */

package pt.webdetails.cdc.hazelcast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;

import com.hazelcast.config.Config;
import com.hazelcast.config.MapConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.examples.TestApp;

public class CfgTestApp extends TestApp {

  protected HazelcastInstance hazelcast;
  protected Map<String, Command> commands;

  public CfgTestApp(HazelcastInstance hazelcast, String[] args) {
    super(hazelcast);
    this.hazelcast = hazelcast;
    initCommands();
    hazelcast.getConfig().setCheckCompatibility(false);//TODO
    if(args.length > 0){
      handleCommand("ns " + args[0]);
    }
  }

  protected void initCommands() {

    commands = new HashMap<String, Command>();

    this.new Command("m.binKeys", "shows serialized map keys", "") {
      public void run() {
        for(Object key: getMap().keySet()){
          println(key);
          byte[] bytes = serializeObject(key);
          println(toHexString(bytes));
          println("#######################");
        }
      }
    };

    this.new Command("m.dumpKeys", "prints serialized map keys to a file", "") {
      public void run() {
        String fileName = getMap().getName() + "_" + System.currentTimeMillis() + ".txt";
        File dumpFile = new File(fileName);
        FileOutputStream fos = null;
        try{
          dumpFile.createNewFile();
          fos = new FileOutputStream(dumpFile);
          ArrayList<String> lines = new ArrayList<String>();
          for(Object key: getMap().keySet()){
            lines.add(key.toString());
          }
          IOUtils.writeLines(lines, System.getProperty("line.separator"), fos);
          println("keys dumped to " + dumpFile.getName());
        } catch (FileNotFoundException e) {
          println("");
        } catch (IOException e) {
          println("error creating file: " + e.getMessage());
        }
        finally{
          IOUtils.closeQuietly(fos);
        }
      }
    };

    this.new Command("m.config", "shows map configuration", "") {
      public void run() {
        String mapName = getMap().getName();
        MapConfig mapConfig = hazelcast.getConfig().getMapConfig(mapName);
        println(formatConfig(mapConfig.toString()));
      }
    };

    this.new Command("config", "show full configuration", "") {
      public void run() {
        Config cfg = hazelcast.getConfig();
        println(formatConfig(cfg.toString()));
      }
    };
  }

  protected void handleCommand(String command) {
    if(command == null) command = "";
    command = command.trim();
      
    if(commands.containsKey(command)) {
      commands.get(command).run();
    }
    else super.handleCommand(command);
  }

  protected void handleHelp(String command) {
    final int DESC_COL = 37;
    super.handleHelp(command);
    println("-- Extensions:");
    for (Command cmd : commands.values()) {
      String invoc = cmd.cmd + cmd.argsHelp;
      String separator = StringUtils.leftPad("//" , DESC_COL - invoc.length());
      println(invoc + separator + cmd.help);
    }
    println("");
  }

  public static String toHexString(byte[] bytes) {
    Formatter formatter = new Formatter();
    try {
      for (byte b : bytes) {
        formatter.format("%02x", b);
      }
      return formatter.toString();
    } finally {
      formatter.close();
    }
  }
  
  private byte[] serializeObject(Object obj){
    ByteArrayOutputStream bos = null;
    ObjectOutputStream out = null;
    try {
      bos = new ByteArrayOutputStream();
      out = new ObjectOutputStream(bos);
      out.writeObject(obj);
      out.flush();
      bos.flush();
      return bos.toByteArray();
    } catch (IOException e) {
      println(e);
      return null;
    }
    finally {
      IOUtils.closeQuietly(out);
      IOUtils.closeQuietly(bos);
    }
  }
  
  public static void main(String[] args) throws Exception {
    CfgTestApp testApp = new CfgTestApp(Hazelcast.newHazelcastInstance(), args);
    testApp.start(args);
  }

  abstract class Command implements Runnable {
    public String cmd;
    public String help;
    public String argsHelp;

    public Command(String command, String help, String argsHelp) {
      assert command != null;
      this.cmd = command;
      this.help = help;
      this.argsHelp = argsHelp;
      commands.put(cmd, this);
    }
  }

  //simple format, TODO: use some lib
  private String formatConfig(String config) {
    StringReader reader = new StringReader(config);
    StringBuilder sbuild = new StringBuilder();
    int ch;
    int indent = 0;
    final int INDENT_FACT = 4;
    try {
      while ((ch = reader.read()) != -1){
        switch(ch) {
          case '{':
          case '[':
            indent++;
            sbuild.append((char) ch);
            sbuild.append('\n');
            sbuild.append(StringUtils.leftPad("", indent * INDENT_FACT));
            break;
          case '}':
          case ']':
            indent--;
            sbuild.append('\n');
            sbuild.append(StringUtils.leftPad("", indent * INDENT_FACT));
            sbuild.append((char) ch);
            break;
          case ',':
            sbuild.append((char) ch);
            sbuild.append('\n');
            sbuild.append(StringUtils.leftPad("", indent * INDENT_FACT));
            ch = reader.read();
            if(ch != ' ') sbuild.append((char)ch);
            break;
          default:
            sbuild.append((char)ch);
        }
      }
    } catch (IOException e) {
      println(e);
    }
    return sbuild.append('\n').toString();
  }

}
