package org.atennert.homectrl.communication;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import org.atennert.com.communication.ICommunicatorAccess;
import org.atennert.com.util.DataContainer;
import org.atennert.homectrl.DataType;
import org.atennert.homectrl.event.EControlUpdate;
import org.atennert.homectrl.event.EventBus;
import org.atennert.homectrl.registration.DataDescription;
import org.atennert.homectrl.registration.IHostAddressBook;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;

public class CommunicationHandlerTest
{
    private CommunicationHandler handler;

    @Mock
    private EventBus eventBus;
    @Mock
    private ICommunicatorAccess communicator;
    @Mock
    private IHostAddressBook addressBook;

    @Before
    public void setUp()
    {
        initMocks( this );

        handler = new CommunicationHandler();

        handler.setEventBus( eventBus );
        handler.setCommunicator( communicator );
        handler.setHostLibrary( addressBook );
    }

    @Test
    public void dontSendCommandsForUnknownActors()
    {
        final EControlUpdate event = new EControlUpdate( 1, 3 );
        when( addressBook.getHostInformation( event.actorId ) ).thenReturn( null );

        handler.control( event );

        verify( communicator, never() ).send( anyString(), any( DataContainer.class ) );
    }

    @Test
    public void sendCommandForActorUpdate()
    {
        final EControlUpdate event = new EControlUpdate( 1, 3 );
        final DataDescription dataDesc = new DataDescription( 1, DataType.INTEGER, "host", "3", 1 );
        when( addressBook.getHostInformation( event.actorId ) ).thenReturn( dataDesc );

        handler.control( event );

        verify( communicator ).send( eq( dataDesc.hostName ),
                argThat( new ArgumentMatcher<DataContainer>()
                {
                    @Override
                    public boolean matches( Object data )
                    {
                        return ((DataContainer) data).dataId.equals( dataDesc.referenceId )
                                && ((DataContainer) data).data.equals( event.value );
                    }
                } ) );
    }

    // TODO implement Tests for receiving updates
}