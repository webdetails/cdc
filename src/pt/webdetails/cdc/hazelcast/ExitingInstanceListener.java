package pt.webdetails.cdc.hazelcast;

import com.hazelcast.core.InstanceEvent;
import com.hazelcast.core.InstanceListener;

public class ExitingInstanceListener implements InstanceListener {

  private int exit=1;
  
  public ExitingInstanceListener(int exitCode){
    exit = exitCode;
  }
  
  @Override
  public void instanceCreated(InstanceEvent arg0) {}

  @Override
  public void instanceDestroyed(InstanceEvent arg0) {
    System.out.println(">> Exiting <<");
    System.exit(exit);
  }

}
