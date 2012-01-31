/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package pt.webdetails.cdc;

/**
 *
 * @author diogo
 */
public class StatusMessage {
  private String code;
  private String message;
  
  public StatusMessage() {}
  public StatusMessage(String code, String message) {
    this.code = code;
    this.message = message;
  }
  
  public String getCode() {
    return code;
  }
  public void setCode(String code) {
    this.code = code;
  }
  public String getMessage() {
    return message;
  }
  public void setMessage(String message) {
    this.message = message;
  }
}
