package org.apache.maven.plugin.surefire.extensions;

import org.apache.maven.plugin.surefire.log.api.ConsoleLogger;
import org.apache.maven.surefire.booter.spi.SurefireMasterProcessChannelProcessorFactory;
import org.apache.maven.surefire.eventapi.Event;
import org.apache.maven.surefire.extensions.EventHandler;
import org.apache.maven.surefire.extensions.util.CountdownCloseable;
import org.apache.maven.surefire.providerapi.MasterProcessChannelEncoder;
import org.apache.maven.surefire.report.ConsoleOutputCapture;
import org.apache.maven.surefire.report.ConsoleOutputReceiver;
import org.junit.Test;

import javax.annotation.Nonnull;
import java.io.Closeable;
import java.io.IOException;
import java.io.PrintStream;
import java.util.concurrent.TimeUnit;

import static org.mockito.Mockito.mock;

public class E2ETest
{
    private static final String LONG_STRING =
        "0123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789";

    @Test
    public void test() throws Exception
    {
        ConsoleLogger logger = mock( ConsoleLogger.class );
        SurefireForkChannel server = new SurefireForkChannel(1, logger );

        final String connection = server.getForkNodeConnectionString();

        SurefireMasterProcessChannelProcessorFactory factory = new SurefireMasterProcessChannelProcessorFactory();
        factory.connect( connection );
        final MasterProcessChannelEncoder encoder = factory.createEncoder();

        Thread t = new Thread()
        {
            @Override
            public void run()
            {
                ConsoleOutputReceiver target = new ConsoleOutputReceiver()
                {
                    @Override
                    public void writeTestOutput( String output, boolean newLine, boolean stdout )
                    {
                        encoder.stdOut( output, true );
                    }
                };

                PrintStream out = System.out;
                PrintStream err = System.err;

                ConsoleOutputCapture.startCapture( target );

                try
                {
                    for ( int i = 0; i < 320_000; i++ )
                    {
                        System.out.println( LONG_STRING );
                    }
                    System.setOut( out );
                    System.setErr( err );
                    TimeUnit.MINUTES.sleep( 1L );
                }
                catch ( Exception e )
                {
                    e.printStackTrace();
                }
            }
        };
        t.setDaemon( true );
        t.start();

        server.connectToClient();

        EventHandler<Event> h = new EventHandler<Event>()
        {
            volatile int i;
            volatile long t1;

            @Override
            public void handleEvent( @Nonnull Event event )
            {
                try
                {
                    if ( i++ == 0 )
                    {
                        t1 = System.currentTimeMillis();
                    }

                    if ( i == 320_000 )
                    {
                        long t2 = System.currentTimeMillis();
                        TimeUnit.SECONDS.sleep( 1L );
                        System.out.println( "Forked JVM spent "
                            + ( t2 - t1 )
                            + "ms on transferring all lines of the log." );
                    }
                }
                catch ( InterruptedException e )
                {
                    e.printStackTrace();
                }
            }
        };

        Closeable c = new Closeable()
        {
            @Override
            public void close() throws IOException
            {

            }
        };

        server.bindEventHandler( h, new CountdownCloseable( c, 1 ), null )
        .start();

        TimeUnit.SECONDS.sleep( 60L );

        factory.close();
        server.close();
    }
}
