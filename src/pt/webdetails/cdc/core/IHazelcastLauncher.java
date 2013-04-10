package pt.webdetails.cdc.core;

/**
 * Launches a non-lite hazelcast instance.
 */
public interface IHazelcastLauncher {

  public abstract void start();

  public abstract void stop();

  public abstract boolean isRunning();

}