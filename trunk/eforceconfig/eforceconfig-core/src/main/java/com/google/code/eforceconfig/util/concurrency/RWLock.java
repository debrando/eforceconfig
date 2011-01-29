package com.google.code.eforceconfig.util.concurrency;

import org.apache.log4j.Logger;

public class RWLock
{
   private static Logger logger= Logger.getLogger(RWLock.class);
   private int givenLocks;
   private int waitingWriters;
   private Object mutex;
   private ThreadLocal flag;
   
   public RWLock()
   {
       mutex= new Object();
       flag= new ThreadLocal()
             {
                   protected synchronized Object initialValue()
                   {
                            return Boolean.valueOf(false);
                   }
             };
       givenLocks= 0;
       waitingWriters= 0;
   }
   
 
   public Lock getReadLock()
   {
       synchronized(mutex)
       {
             if (getFlag())
               throw new RuntimeException("this thread has already acquired a lock on this object");

             while(givenLocks==-1||waitingWriters!=0)
             try
             { 
                 mutex.wait();
             } 
             catch (InterruptedException e)
             { 
                throw new RuntimeException(e);
             }
    
             givenLocks++;
         
             setFlag();
             
             return new Lock();
       }
   }
 
   public Lock getWriteLock()
   {
       synchronized(mutex)
       {
            if (getFlag())
              throw new RuntimeException("this thread has already acquired a lock on this object");

            waitingWriters++;
    
            while (givenLocks!=0)
               try
               { 
                   mutex.wait();
               }
               catch (InterruptedException e)
               { 
                   waitingWriters--;
                   throw new RuntimeException(e);
               }
    
            waitingWriters--;
            givenLocks = -1;

            setFlag();

            return new Lock();
       }
   }

   public void releaseLock(Lock lock)
   {
       if (lock.released)
           throw new RuntimeException("lock already released");
           
       synchronized (mutex)
       {
             if (!getFlag())
               throw new RuntimeException("this thread has no lock on this object");
           
             if (givenLocks==-1)
               givenLocks= 0;
             else
               givenLocks--;
        
             flag.set(Boolean.valueOf(false));
             
             mutex.notifyAll();
       }

       lock.released= true;
   }
   
   private boolean getFlag()
   {
       return ((Boolean)flag.get()).booleanValue();
   }
   
   private void setFlag()
   {
       flag.set(Boolean.valueOf(true));
   }
   
   public class Lock
   {
        private boolean released= false;
        private RuntimeException ex= new RuntimeException("lock released by finalizer");
        
        protected void finalize()
        throws Throwable
        { 
            if (!released)
            {
                logger.error("lock not released by code, releasing and throwing exception:",ex);                
                releaseLock(this);
                throw ex;
            }
            else
                logger.debug("lock released by code");
        }
   }
}
