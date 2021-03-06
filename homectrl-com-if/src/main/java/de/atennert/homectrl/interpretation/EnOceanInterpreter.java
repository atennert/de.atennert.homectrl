package de.atennert.homectrl.interpretation;

import de.atennert.com.communication.IDataAcceptance;
import de.atennert.com.interpretation.IInterpreter;
import de.atennert.com.util.DataContainer;
import de.atennert.com.util.MapDataContainer;
import de.atennert.com.util.MessageContainer;
import de.atennert.com.util.Session;
import de.atennert.homectrl.communication.Base64Coder;
import de.atennert.homectrl.communication.CodingHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Scheduler;
import rx.Single;
import rx.SingleSubscriber;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

/**
 * Interpreter for EnOcean data.
 */
public class EnOceanInterpreter implements IInterpreter
{

    private static Logger log = LoggerFactory.getLogger( EnOceanInterpreter.class );

    /** Radio telegram */
    private static final int TYPE_RADIO = 0x01;
    /** Radio subtelegram */
    private static final int TYPE_RADIO_SUB_TEL = 0x03;
    /** Advanced radio protocol raw data */
    private static final int TYPE_RADIO_ADVANCED = 0x0A;

    public DataContainer decode( MessageContainer message )
    {
        // not used
        return null;
    }

    public String encode( DataContainer data )
    {
        // not used
        return null;
    }

    @Override
    public void interpret(MessageContainer msgContainer, Session session, IDataAcceptance acceptance, Scheduler scheduler) {
        log.trace( "interpreting: " + msgContainer.message );
        if( msgContainer.hasException() || msgContainer.message == null )
        {
            session.call(null);
        }

        // message = packetType, isDataValid, lengthOptional, optional[], data[]
        final byte[] byteMessage = Base64Coder.decode( msgContainer.message );

        final int optLen = CodingHelper.byteToInt( byteMessage[2] );
        final byte[] optional = new byte[optLen], data = new byte[byteMessage.length - 3 - optLen];

        System.arraycopy( byteMessage, 3, optional, 0, optLen );
        System.arraycopy( byteMessage, 3 + optLen, data, 0, data.length );

        final int packetType = CodingHelper.byteToInt( byteMessage[0] );

        final String senderID = getSenderID( packetType, data );

        final Map<String, Object> msgData = new HashMap<>();

        msgData.put( "time", Calendar.getInstance().getTime() );
        msgData.put( "type", packetType );
        msgData.put( "optional", optional );
        msgData.put( "data", data );
        msgData.put( "sender", senderID );
        msgData.put( "valid", byteMessage[1] == 1);

        acceptance.accept( senderID, new MapDataContainer(msgData, new SingleSubscriber<DataContainer>() {
            @Override
            public void onSuccess(DataContainer dataContainer) {
                Single.<String>from(null)
                        .subscribeOn(session.scheduler)
                        .subscribe(session);
            }

            @Override
            public void onError(Throwable throwable) {
                log.error("[EnOceanInterpreter.Subscriber.accept] ERROR");
                Single.<String>from(null)
                        .subscribeOn(session.scheduler)
                        .subscribe(session);
            }
        }) );
    }

    private static String getSenderID(final int packetType, final byte[] data )
    {
        final byte[] addrBytes = new byte[4];
        switch( packetType )
        {
        case TYPE_RADIO:
        case TYPE_RADIO_SUB_TEL:
        case TYPE_RADIO_ADVANCED:
            System.arraycopy( data, data.length - 5, addrBytes, 0, 4 );
            return getAddressString( addrBytes );
        default:
            // Nothing to do
        }
        return null;
    }

    private static String getAddressString(final byte[] addrBytes )
    {
        int addr = 0, cnt = 3;
        for( final byte b : addrBytes )
        {
            // some IDEs say the "0xFFFFFFFF &" can be removed ... DON'T REMOVE IT, IT'LL BREAK!!!
            addr |= 0xFFFFFFFF & (b << (cnt-- * 8));
        }
        final String number = Integer.toHexString( addr );
        return "00000000".substring( number.length() ).concat( number );
    }
}
