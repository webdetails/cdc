package pt.webdetails.cdc.ws;

public class Result {
  
  public enum Status {
    OK,
    ERROR
  }
  
  private Status status;
  private Object result;
  
  public Result() {}
  public Result(Status status, Object result) {
    this.status = status;
    this.result = result;
  }
  
  public String getStatus() {
    return "" + status;
  }
  public void setStatus(Status status) {
    this.status = status;
  }
  public Object getResult() {
    return result;
  }
  public void setResult(Object result) {
    this.result = result;
  }
  
  public static Result getFromException(Exception e){
    return getError(e.getLocalizedMessage());
  }
  public static Result getOK(Object result){
    return new Result(Status.OK, result);
  }
  public static Result getError(String msg){
    return new Result(Status.ERROR, msg);
  }
  
}
