package pt.webdetails.cdc.cda;

import java.util.concurrent.Callable;

/**
 * Boilerplate to run a method in a different ClassLoader.
 */
public class ClassLoaderAwareCaller {
  private ClassLoader classLoader;
  
  public ClassLoaderAwareCaller(ClassLoader classLoader){
   this.classLoader = classLoader; 
  }
  
  protected <T> T callInClassLoader(Callable<T> callable) throws Exception{
    ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
    try
    {
      if(this.classLoader != null)
      {
        Thread.currentThread().setContextClassLoader(this.classLoader);
      }
      
      return callable.call();
      
    }
    finally{
      Thread.currentThread().setContextClassLoader(contextClassLoader);
    }
  }
      
  protected void runInClassLoader(Runnable runnable)
  {
    ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
    try
    {
      if(this.classLoader != null)
      {
        Thread.currentThread().setContextClassLoader(this.classLoader);
      }
      
      runnable.run();
      
    }
    finally{
      Thread.currentThread().setContextClassLoader(contextClassLoader);
    }
  }
}
