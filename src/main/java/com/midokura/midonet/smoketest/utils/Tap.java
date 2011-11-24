/*
* Copyright 2011 Midokura Europe SARL
*/


package com.midokura.midonet.smoketest.utils;

import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Structure;
import com.sun.jna.Union;

public class Tap {
	private final static Logger log = LoggerFactory
    		.getLogger(Tap.class);
	/* if.h */
    public static final int IFNAMSIZ = 16;
    
    /* if_tun.h */
    public static final int TUNSETIFF = 0x400454ca;
    public static final int TUNSETPERSIST = 0x400454cb;
    
    public static final int TUNSETOWNER = 0x400454cc;
    public static final int TUNSETGROUP = 0x400454ce;
    
    public static final int IFF_TUN = 0x0001;
    public static final int IFF_TAP = 0x0002;
    public static final int IFF_NO_PI = 0x1000;
    
    /* ioctls.h */
    public static final int SIOCGIFHWADDR = 0x8927;		/* Get hardware address		*/
    public static final int SIOCSIFHWADDR =	0x8924;		/* set hardware address 	*/
    
    /* fcntl.h */
    public static final int O_RDONLY = 0;
    public static final int O_WRONLY = 1;
    public static final int O_RDWR = 2;
    public static final int O_NONBLOCK = 04000;
    public static final int EWOULDBLOCK = 11;

    public static class InterfaceReq extends Structure{
    	
        public InterfaceReqName ifr_ifrn;
        public InterfaceReqParam ifr_ifru;
        public static class InterfaceReqByRef extends InterfaceReq implements Structure.ByReference { }
        
        public static class InterfaceReqName extends Union {
            public byte[] ifrn_name = new byte[IFNAMSIZ];
            public static class ByReference extends Union implements Structure.ByReference {
            }
        }

        public static class InterfaceReqParam extends Union {
            public SockAddr ifru_hwaddr;
            public SockAddr ifru_addr;
            public SockAddr ifru_dstaddr;
            public SockAddr ifru_broadaddr;
            public SockAddr ifru_netmask;
        	public short ifru_flags;
            public int ifru_ivalue;
            public int ifru_mtu;
            public byte[] ifru_slave = new byte[IFNAMSIZ];
            public byte[] ifru_newname = new byte[IFNAMSIZ];
            public static class ByReference extends Union implements Structure.ByReference {
            }
        }
        
        public static class SockAddr extends Structure {
        	public short sa_family_t;
        	public byte[] sa_data = new byte[14];
        }
    }
    /*
     * Open a TAP interface.

       Args:
         name: The name of the interface to create, e.g. 'tap0'. If empty the name of the tap created
               will be copied

       Returns:
        The file descriptor of the open socket to the TAP interface.    
     */
    public static int openTap(String tapName, boolean nonblocking)
    {
    	// check if the the parameter is valid
        if(tapName.length() > IFNAMSIZ)
        {
        	throw new RuntimeException("Tap name too long!");
        }
        
    	String tunPath = "/dev/net/tun";
    	int flags = nonblocking ? O_NONBLOCK : 0;
    	int fd = TestLibrary.INSTANCE.open(tunPath, flags | O_RDWR);
    	if(fd < 0)
    		throw new RuntimeException("Problem in opening /dev/net/tun");
        
    	InterfaceReq interfaceReq = new InterfaceReq();      
        interfaceReq.ifr_ifru.setType("ifru_flags");
        interfaceReq.ifr_ifrn.setType("ifrn_name");
        interfaceReq.ifr_ifru.ifru_flags = IFF_TAP | IFF_NO_PI;

        if(!tapName.isEmpty())
        	interfaceReq.ifr_ifrn.ifrn_name = tapName.getBytes();
        int result = TestLibrary.INSTANCE.ioctl(fd, TUNSETIFF, interfaceReq);
        int err = Native.getLastError();
        // check if ioctl has been successful
        if(result < 0)
        	throw new RuntimeException("Problem in opening tap, ioctl failed");
        
        
        /* get the name */
        if(tapName.isEmpty())
        	tapName = interfaceReq.ifr_ifrn.ifrn_name.toString();
        
        return fd;
        
        /* ALTERNATIVE
      	NativeLibrary unix = NativeLibrary.getInstance("c");
    	Function ioctl = unix.getFunction("ioctl");
    	
    	int res2 = ioctl.invokeInt(new Object[]{fd, 0x400454ca, interfaceReq});  
         */
    }
    public static void openPersistentTapSetOwner(String tapName, int owner)
    {
         openPersistentTap(tapName, owner, -1, "");
    }
    
    public static void openPersistentTapSetGroup(String tapName, int group)
    {
    	 openPersistentTap(tapName, -1, group, "");
    }
    
    public static void openPersistentTap(String tapName)
    {
    	 openPersistentTap(tapName, -1, -1, "");
    }
    
    public static void openPersistentTap(String tapName, String hwAddr)
    {
    	 openPersistentTap(tapName, -1, -1, hwAddr);
    }
    
    /*
     *  Create or setup a persistent TAP interface.

    If the interface already exists, it is setup with the given owner and group
    and set persistent.

    Creating a new interface requires running as user root, or with Linux
    capability CAP_NET_ADMIN.

    Args:
        name: If not None, the name of the interface to create, e.g. 'tap0'
        owner: The UID (user id) of the user owning the interface, as an
            integer value.  If None, the interface has no owner and is usable
            only by root.
        group: The GID (group id) of the group owning the interface, as an
            integer value.  If None, the interface has no owner and is usable
            only by root.
        mac: The MAC address of the interface, as a string in the form
            '01:23:45:67:89:ab'.  If None, an address is randomly chosen.

    Returns:
        The name of the interface actually created.

    Raises:
        RunTimeException: If the interface cannot be created or setup or if user/group cannot be set.
    """

     *  
     */
    public static void openPersistentTap(String tapName, int owner, int group, String hwAddr)
    {
    	int res;
    	int fd = openTap(tapName, false);
    	    	
    	/* change owner */
    	if( owner >= 0 )
    	{
    		res = TestLibrary.INSTANCE.ioctl(fd, TUNSETOWNER, owner);
    	if(res < 0)
    		throw new RuntimeException("ioctl failed in setting the owner!");
    	}
    	
    	/* change group */
    	if( group >= 0 )
    	{
    		res = TestLibrary.INSTANCE.ioctl(fd, TUNSETGROUP, group);
        	if(res < 0)
        		throw new RuntimeException("ioctl failed in setting the group!");
    	}	
    	
    	/* make it persistent */
    	res = TestLibrary.INSTANCE.ioctl(fd, TUNSETPERSIST, 1);   	
    	if(res < 0)
    		throw new RuntimeException("ioctl failed in making the tap persistent!");
    	
    	/* change hw address */
    	if( !hwAddr.isEmpty())
    	{
    		setHwAddress(fd, tapName, hwAddr);
    	}
    	
    	TestLibrary.INSTANCE.close(fd);

    }
    
    /*
     *   Set the hardware address of a network interface.

    	Args:
        	name: The name of the network interface, e.g. 'eth0'.
        	hwAddress: address to set in the form '01:23:45:67:89:ab' 
        Raises:
        RunTimeException: If the some parameter is invalid or if the hw address cannot be changed.	 
     */
    public static void setHwAddress(String tapName, String hwAddress)
    {
    	int fd = openTap(tapName, false);
    	setHwAddress(fd, tapName, hwAddress);
    	TestLibrary.INSTANCE.close(fd);
    }
    
    /*
     *   Set the hardware address of a network interface.

    	Args:
    		fd: opened file descriptor for the tap
        	name: The name of the network interface, e.g. 'eth0'.
        	hwAddress: address to set in the form '01:23:45:67:89:ab' 
        Raises:
        RunTimeException: If the interface cannot be destroyed, e.g. it doesn't exist.	 
     */
    public static void setHwAddress(int fd, String tapName, String hwAddress)
    {
    	
    	if( fd < 0 | tapName.isEmpty() | hwAddress.isEmpty())
    		throw new RuntimeException("Invalid Parameter!");
    		
    	/* Prepare structure ifr_ifru */
    	
        InterfaceReq interfaceReq = new InterfaceReq();      
        interfaceReq.ifr_ifru.setType("ifru_hwaddr");
        interfaceReq.ifr_ifrn.setType("ifrn_name");
        
        /*  TapName */
        interfaceReq.ifr_ifrn.ifrn_name = tapName.getBytes();
        
        /* HW address */
        String[] bytes = hwAddress.split(":");
       // byte[] parsed = new byte[bytes.length];
        if(bytes.length != 6)
        	throw new RuntimeException("Invalid MAC address!");
        
        for (int x = 0; x < bytes.length; x++)
        {
            
            Integer value = Integer.parseInt(bytes[x], 16);
            /* get just the first byte */
            byte bValue = value.byteValue();
            interfaceReq.ifr_ifru.ifru_hwaddr.sa_data[x] = bValue;
        }
        
        /* copy hw address family */
        InterfaceReq hwStruct = new InterfaceReq();
        getHwAddress(fd, tapName, hwStruct);
        interfaceReq.ifr_ifru.ifru_hwaddr.sa_family_t = hwStruct.ifr_ifru.ifru_hwaddr.sa_family_t;
        
    	/* change MAC */
    	int res = TestLibrary.INSTANCE.ioctl(fd, SIOCSIFHWADDR, interfaceReq);
    	int err = Native.getLastError();
    	if(res < 0)
    		throw new RuntimeException("ioctl failed in changing the MAC address!");
    }
    
    
    /*
     *   Get the hardware address of a network interface.

    	Args:
        	name: The name of the network interface, e.g. 'eth0'.
        	hwStruct: struct filled by ioctl call

    	Returns:
        	The MAC address of the interface, in the form '01:23:45:67:89:ab'.
  
     */
    public static String getHwAddress(String tapName)
    {
    	int fd = openTap(tapName, false);
    	InterfaceReq hwStruct = new InterfaceReq();
    	String hwAddr = getHwAddress(fd, tapName, hwStruct);
    	TestLibrary.INSTANCE.close(fd);
    	return hwAddr;
    }
    
    /*
     *   Get the hardware address of a network interface.

    	Args:
    		fd: opened file descriptor for the tap
        	name: The name of the network interface, e.g. 'eth0'.
        	hwStruct: struct filled by ioctl call

    	Returns:
        	The MAC address of the interface, in the form '01:23:45:67:89:ab'.
  
     */
    public static String getHwAddress(int fd, String tapName, InterfaceReq hwStruct)
    {
    	if( fd < 0 | tapName.isEmpty())
    		throw new RuntimeException("Invalid Parameter!");
    	
        /* clear struct */
        hwStruct.clear();
        
        hwStruct.ifr_ifru.setType("ifru_hwaddr");
        hwStruct.ifr_ifrn.setType("ifrn_name");
            
        /*  TapName */
        hwStruct.ifr_ifrn.ifrn_name = tapName.getBytes();
        
        int res = TestLibrary.INSTANCE.ioctl(fd, SIOCGIFHWADDR, hwStruct);
    	if(res < 0)
    		throw new RuntimeException("ioctl failed in getting the MAC address!");
    	
    	/* Format String */
        StringBuilder sb = new StringBuilder(18);
        /* only the first 6 bytes are valid, the rest are 0s */
        for(int i = 0; i < 6; i++)
        {
        	byte b = hwStruct.ifr_ifru.ifru_hwaddr.sa_data[i];
            if (sb.length() > 0)
                sb.append(':');
            sb.append(String.format("%02x", b));       	
        }
        
        return sb.toString();
      
    }
    
/*
 *  Destroys a persistent TAP interface.

    Make the interface with the given name non-persistent, which in effect
    destroys it.

    Args:
        name: The name of the interface to destroy.

    Raises:
        RunTimeException: If the interface cannot be destroyed, e.g. it doesn't exist.
 */
    public static void destroyPersistentTap(String name)
    {
    	int fd = openTap(name, false);
    	int res = TestLibrary.INSTANCE.ioctl(fd, TUNSETPERSIST, 0);
    	TestLibrary.INSTANCE.close(fd);
    	if(res < 0)
    		throw new RuntimeException("ioctl failed in destroying the tap!");   	
    }

    public static void closeFD(int fd) {
    	Native.setLastError(0);
    	int close_res = TestLibrary.INSTANCE.close(fd);
    	if (close_res < 0) {
        	int err = Native.getLastError();
    		log.error("Closing write fd returned {} and last error is {}",
    				close_res, err);
    	}
    }

    public static void writeToTap(String name, byte[] buffer, int nBytes)
    {
    	int fd = openTap(name, false);
    	//log.debug("Opened tap {} write-blocking returned fd:{}", name, fd);
    	int res = TestLibrary.INSTANCE.write(fd, buffer, nBytes);
    	if(res < 0)
    		throw new RuntimeException("write failed!");
    	closeFD(fd);
    }
    
    public static void writeToTap(int fd, byte[] buffer, int nBytes)
    {
    	long maxSleepMillis = 1000;
    	long timeSlept = 0;
    	while (nBytes > 0) {
        	Native.setLastError(0);
    		int res = TestLibrary.INSTANCE.write(fd, buffer, nBytes);
        	int err = Native.getLastError();    		
    		if(res < 0) {
        		if (err != EWOULDBLOCK)
        			throw new RuntimeException(
        					String.format("write failed with code %d!", err));
    		}
    		if (res > 0) {
    			nBytes -= res;
    			if (0 == nBytes)
    				return;
    			buffer = Arrays.copyOfRange(buffer, res, nBytes);
    		}
    		if (timeSlept >= maxSleepMillis)
    			throw new RuntimeException("write timed out");
            try {
            	log.debug("Sleeping for 100 millis.");
                Thread.sleep(100);
            } catch (InterruptedException e) {
                log.error("InterrupException in recv()", e);
            }
            timeSlept += 100;
    	}
    }
    
    public static int readFromTap(String name, byte[] buffer, int nBytes)
    {
    	int fd = openTap(name, true);
    	//log.debug("Opened tap {} nonblocking returned fd:{}", name, fd);
    	Native.setLastError(0);
    	int res = TestLibrary.INSTANCE.read(fd, buffer, nBytes);
    	int err = Native.getLastError();
    	closeFD(fd);
    	if(res < 0) {
    		if (err != EWOULDBLOCK)
    			throw new RuntimeException(
    					String.format("read failed with code %d!", err)); 
    		return 0;
    	}
    	return res;
    }
    
    public static int readFromTap(int fd, byte[] buffer, int nBytes)
    {
    	//log.debug("Opened tap {} nonblocking returned fd:{}", name, fd);
    	Native.setLastError(0);
    	int res = TestLibrary.INSTANCE.read(fd, buffer, nBytes);
    	int err = Native.getLastError();
    	if(res < 0) {
    		if (err != EWOULDBLOCK)
    			throw new RuntimeException(
    					String.format("read failed with code %d!", err)); 
    		return 0;
    	}
    	return res;
    }
    
    public interface TestLibrary extends Library {
        TestLibrary INSTANCE = (TestLibrary)
                Native.loadLibrary("c", TestLibrary.class);

        int ioctl(int fd, int code, InterfaceReq req);
        int ioctl(int fd, int code, int owner);
        
        int open(String name, int flags);
        int close(int fd);
        int fcntl(int fd, int cmd);
        int read(int handle, byte[] buffer, int nbyte);
        int write(int handle,byte[] buffer,  int  nbyte);

    }
    

}

