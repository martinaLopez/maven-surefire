package org.apache.maven.surefire.util.internal;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.NonWritableChannelException;
import java.nio.channels.WritableByteChannel;

import static java.nio.file.Files.readAllBytes;
import static org.fest.assertions.Assertions.assertThat;

/**
 * The tests for {@link Channels#newChannel(OutputStream)} and {@link Channels#newBufferedChannel(OutputStream)}.
 */
public class ChannelsWriterTest
{
    @Rule
    public final ExpectedException ee = ExpectedException.none();

    @Rule
    public final TemporaryFolder tmp = TemporaryFolder.builder()
        .assureDeletion()
        .build();

    @Test
    public void wrappedBuffer() throws Exception
    {
        final boolean[] isFlush = {false};
        ByteArrayOutputStream out = new ByteArrayOutputStream()
        {
            @Override
            public void flush() throws IOException
            {
                isFlush[0] = true;
                super.flush();
            }
        };
        WritableByteChannel channel = Channels.newBufferedChannel( out );
        ByteBuffer bb = ByteBuffer.wrap( new byte[] {1, 2, 3} );
        int countWritten = channel.write( bb );
        assertThat( countWritten )
            .isEqualTo( 3 );

        assertThat( out.toByteArray() )
            .hasSize( 3 )
            .isEqualTo( new byte[] {1, 2, 3} );

        assertThat( isFlush )
            .hasSize( 1 )
            .containsOnly( true );

        assertThat( bb.position() )
            .isEqualTo( 3 );

        assertThat( bb.limit() )
            .isEqualTo( 3 );

        assertThat( bb.capacity() )
            .isEqualTo( 3 );

        assertThat( channel.isOpen() )
            .isTrue();
    }

    @Test
    public void bigBuffer() throws Exception
    {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        WritableByteChannel channel = Channels.newChannel( out );
        ByteBuffer bb = ByteBuffer.allocate( 4 );
        bb.put( (byte) 1 );
        bb.put( (byte) 2 );
        bb.put( (byte) 3 );
        int countWritten = channel.write( bb );
        assertThat( countWritten ).isEqualTo( 3 );
        assertThat( out.toByteArray() )
            .hasSize( 3 )
            .isEqualTo( new byte[] {1, 2, 3} );

        assertThat( bb.position() )
            .isEqualTo( 3 );

        assertThat( bb.limit() )
            .isEqualTo( 3 );

        assertThat( bb.capacity() )
            .isEqualTo( 4 );

        assertThat( channel.isOpen() )
            .isTrue();
    }

    @Test
    public void bufferedChannel() throws Exception
    {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        WritableBufferedByteChannel channel = Channels.newBufferedChannel( out );
        ByteBuffer bb = ByteBuffer.allocate( 5 );
        bb.put( (byte) 1 );
        bb.put( (byte) 2 );
        bb.put( (byte) 3 );

        channel.writeBuffered( bb );

        assertThat( out.toByteArray() )
            .isEmpty();

        channel.write( ByteBuffer.allocate( 0 ) );

        assertThat( out.toByteArray() )
            .isEmpty();

        channel.write( ByteBuffer.wrap( new byte[] {4} ) );

        assertThat( out.toByteArray() )
            .hasSize( 4 )
            .isEqualTo( new byte[] {1, 2, 3, 4} );

        assertThat( bb.position() )
            .isEqualTo( 3 );

        assertThat( bb.limit() )
            .isEqualTo( 3 );

        assertThat( bb.capacity() )
            .isEqualTo( 5 );

        assertThat( channel.isOpen() )
            .isTrue();
    }

    @Test
    public void shouldFailAfterClosed() throws IOException
    {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        WritableByteChannel channel = Channels.newChannel( out );
        channel.close();
        assertThat( channel.isOpen() ).isFalse();
        ee.expect( ClosedChannelException.class );
        channel.write( ByteBuffer.allocate( 0 ) );
    }

    @Test
    public void shouldFailIfNotReadable() throws IOException
    {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        WritableByteChannel channel = Channels.newChannel( out );
        ee.expect( NonWritableChannelException.class );
        channel.write( ByteBuffer.allocate( 0 ).asReadOnlyBuffer() );
    }

    @Test
    public void shouldFailIOnDirectBuffer() throws IOException
    {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        WritableByteChannel channel = Channels.newChannel( out );
        ee.expect( NonWritableChannelException.class );
        channel.write( ByteBuffer.allocateDirect( 0 ) );
    }

    @Test
    public void shouldUseFileChannel() throws IOException
    {
        File f = tmp.newFile();
        FileOutputStream os = new FileOutputStream( f );
        WritableByteChannel channel = Channels.newChannel( os );
        ByteBuffer bb = ByteBuffer.wrap( new byte[] {1, 2, 3} );
        channel.write( bb );

        assertThat( channel.isOpen() )
            .isTrue();

        channel.close();

        assertThat( channel.isOpen() )
            .isFalse();

        assertThat( readAllBytes( f.toPath() ) )
            .hasSize( 3 )
            .isEqualTo( new byte[] {1, 2, 3} );
    }
}
