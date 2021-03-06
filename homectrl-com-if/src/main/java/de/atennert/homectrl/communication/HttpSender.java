package de.atennert.homectrl.communication;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;

import de.atennert.com.communication.ISender;
import de.atennert.com.util.MessageContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class sends HTTP messages.
 */
public class HttpSender implements ISender
{

    private static final Logger log = LoggerFactory.getLogger( HttpSender.class );

    private static final String POST_HEADER = "POST / HTTP/1.1\r\n" + "Content-type: text/CONTENT_TYPE\r\n"
            + "Content-length: ";
    private static final String GET_HEADER = "GET / HTTP/1.1\r\n\r\n";

    public MessageContainer send( String address, MessageContainer message )
    {
        log.info( "sending " + message + " to " + address );
        Socket socket;
        PrintWriter out;
        BufferedReader in;

        // separate address parts
        String addressParts[] = address.split( ":" );
        String host = addressParts[0];
        int port = Integer.valueOf( addressParts[1] );

        try
        {
            // open connection
            socket = new Socket( host, port );
            out = new PrintWriter( socket.getOutputStream(), true );
            in = new BufferedReader( new InputStreamReader( socket.getInputStream() ) );
        }
        catch( UnknownHostException e )
        {
            log.error( "Don't know about host: " + host + "." );
            return new MessageContainer(MessageContainer.Exception.UNKOWN_HOST);
        }
        catch( IOException e )
        {
            log.error( "Couldn't get I/O for the connection to: " + host + "." );
            return new MessageContainer(MessageContainer.Exception.IO);
        }

        MessageContainer response;

        try
        {
            if( message != null )
            { // make a POST request
                out.print( POST_HEADER.replace( "CONTENT_TYPE", message.interpreter )
                        + message.message.length() + "\r\n\r\n" + message.message );
            }
            else
            { // make a GET request
                out.print( GET_HEADER );
            }
            out.flush();

            // check for and receive response
            if( in.readLine().matches( "(HTTP/\\d\\.\\d)(\\s)(200)(\\s)(OK)" ) )
            {
                response = HttpHelper.getContent( in );
            }
            else
            {
                response = new MessageContainer(MessageContainer.Exception.EMPTY);
            }

            // close connection
            out.close();
            in.close();
            socket.close();
        }
        catch( IOException e )
        {
            log.error( "I/O for the connection to " + host + " was lost during receiving." );
            return new MessageContainer(MessageContainer.Exception.IO);
        }

        return response;
    }

}
