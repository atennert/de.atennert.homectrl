package de.atennert.homectrl.registration;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import de.atennert.com.registration.INodeRegistration;
import de.atennert.homectrl.util.IAddress;
import org.springframework.beans.factory.annotation.Required;

public class DeviceRegistration implements INodeRegistration, IHostAddressBook
{
    /**
     * Contains the protocol-interpreter restrictions. key: protocol value: set
     * of interpreters
     */
    private Map<String, Set<String>> interpretersForProtocols;

    private Set<DataDescription> dataDescriptions;

    private Map<String, NodeDescription> nodeDescriptions;


    @Required
    public void setInterpreterProtocolRestrictions( Map<String, Set<String>> interpreterProtocolRestrictions )
    {
        interpretersForProtocols = interpreterProtocolRestrictions;
    }

    @Required
    public void setDataDescriptions( Set<DataDescription> dataDescriptions )
    {
        this.dataDescriptions = dataDescriptions;
    }

    @Required
    public void setNodeDescriptions(Set<NodeDescription> nodeDescriptions)
    {
        this.nodeDescriptions = new HashMap<String, NodeDescription>();

        for (NodeDescription node : nodeDescriptions)
        {
            this.nodeDescriptions.put( node.name, node );
        }
    }



    public DataDescription getHostInformation( int deviceId )
    {
        for( DataDescription entry : dataDescriptions )
        {
            if( entry.id == deviceId )
            {
                return entry;
            }
        }
        return null;
    }

    public Set<DataDescription> getHostDevices( String senderAddress, Set<String> referenceIds )
    {
        Set<DataDescription> entries = new HashSet<DataDescription>();

        for (NodeDescription d : nodeDescriptions.values())
        {
            for (IAddress address : d.getSendAddresses())
            {
                if (address.getAddress().equals( senderAddress ))
                {
                    for (DataDescription entry : dataDescriptions)
                    {
                        if (entry.hostName.equals(d.name) && referenceIds.contains(entry.referenceId))
                        {
                            entries.add( entry );
                        }
                    }
                    break;
                }
            }
        }
        return entries;
    }

    public Set<String> getNodeReceiveAddresses( String node )
    {
        Set<IAddress> addressObjects = nodeDescriptions.get( node ).getReceiveAddresses();
        Set<String> addressStrings = new HashSet<String>();
        for (IAddress addressObj : addressObjects)
        {
            addressStrings.add( addressObj.getAddress() );
        }
        return addressStrings;
    }

    public String getNodeReceiveProtocol( String address )
    {
        for( NodeDescription d : nodeDescriptions.values() )
        {
            for (IAddress addressObj : d.getReceiveAddresses())
            {
                if (addressObj.getAddress().equals( address ))
                {
                    return addressObj.getProtocol();
                }
            }
        }
        return null;
    }

    public Set<String> getNodeInterpreters( String node )
    {
        return nodeDescriptions.get( node ).getInterpreters();
    }

    public Set<String> getInterpretersForProtocol( String protocol )
    {
        Set<String> result = interpretersForProtocols.get( protocol );
        return result == null ? new HashSet<String>() : result;
    }
}
