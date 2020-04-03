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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.NonReadableChannelException;
import java.nio.channels.ReadableByteChannel;

import static java.nio.file.Files.write;
import static org.fest.assertions.Assertions.assertThat;

/**
 * The tests for {@link Channels#newChannel(InputStream)} and {@link Channels#newBufferedChannel(InputStream)}.
 */
public class ChannelsReaderTest
{
    @Rule
    public final ExpectedException ee = ExpectedException.none();

    @Rule
    public final TemporaryFolder tmp = TemporaryFolder.builder()
        .assureDeletion()
        .build();

    @Test
    public void exactBufferSize() throws Exception
    {
        ByteArrayInputStream is = new ByteArrayInputStream( new byte[] {1, 2, 3} );
        ReadableByteChannel channel = Channels.newChannel( is );
        ByteBuffer bb = ByteBuffer.allocate( 3 );

        int countWritten = channel.read( bb );

        assertThat( countWritten )
            .isEqualTo( 3 );

        assertThat( bb.arrayOffset() )
            .isEqualTo( 0 );

        assertThat( bb.position() )
            .isEqualTo( 3 );

        assertThat( bb.remaining() )
            .isEqualTo( 0 );

        assertThat( bb.limit() )
            .isEqualTo( 3 );

        assertThat( bb.capacity() )
            .isEqualTo( 3 );

        bb.flip();

        assertThat( bb.arrayOffset() )
            .isEqualTo( 0 );

        assertThat( bb.position() )
            .isEqualTo( 0 );

        assertThat( bb.remaining() )
            .isEqualTo( 3 );

        assertThat( bb.limit() )
            .isEqualTo( 3 );

        assertThat( bb.capacity() )
            .isEqualTo( 3 );

        assertThat( bb.array() )
            .isEqualTo( new byte[] {1, 2, 3} );

        assertThat( channel.isOpen() )
            .isTrue();

        channel.close();

        assertThat( channel.isOpen() )
            .isFalse();
    }

    @Test
    public void bufferedChannel() throws Exception
    {
        ByteArrayInputStream is = new ByteArrayInputStream( new byte[] {1, 2, 3} );
        ReadableByteChannel channel = Channels.newBufferedChannel( is );
        ByteBuffer bb = ByteBuffer.allocate( 4 );

        int countWritten = channel.read( bb );

        assertThat( countWritten )
            .isEqualTo( 3 );

        assertThat( bb.arrayOffset() )
            .isEqualTo( 0 );

        assertThat( bb.position() )
            .isEqualTo( 3 );

        assertThat( bb.remaining() )
            .isEqualTo( 1 );

        assertThat( bb.limit() )
            .isEqualTo( 4 );

        assertThat( bb.capacity() )
            .isEqualTo( 4 );

        bb.flip();

        assertThat( bb.arrayOffset() )
            .isEqualTo( 0 );

        assertThat( bb.position() )
            .isEqualTo( 0 );

        assertThat( bb.remaining() )
            .isEqualTo( 3 );

        assertThat( bb.limit() )
            .isEqualTo( 3 );

        assertThat( bb.capacity() )
            .isEqualTo( 4 );

        assertThat( bb.array() )
            .isEqualTo( new byte[] {1, 2, 3, 0} );

        assertThat( channel.isOpen() )
            .isTrue();

        channel.close();

        assertThat( channel.isOpen() )
            .isFalse();
    }

    @Test
    public void biggerBuffer() throws Exception
    {
        ByteArrayInputStream is = new ByteArrayInputStream( new byte[] {1, 2, 3} );
        ReadableByteChannel channel = Channels.newChannel( is );
        ByteBuffer bb = ByteBuffer.allocate( 4 );

        int countWritten = channel.read( bb );

        assertThat( countWritten )
            .isEqualTo( 3 );

        assertThat( bb.arrayOffset() )
            .isEqualTo( 0 );

        assertThat( bb.position() )
            .isEqualTo( 3 );

        assertThat( bb.remaining() )
            .isEqualTo( 1 );

        assertThat( bb.limit() )
            .isEqualTo( 4 );

        assertThat( bb.capacity() )
            .isEqualTo( 4 );

        bb.flip();

        assertThat( bb.arrayOffset() )
            .isEqualTo( 0 );

        assertThat( bb.position() )
            .isEqualTo( 0 );

        assertThat( bb.remaining() )
            .isEqualTo( 3 );

        assertThat( bb.limit() )
            .isEqualTo( 3 );

        assertThat( bb.capacity() )
            .isEqualTo( 4 );

        assertThat( bb.array() )
            .isEqualTo( new byte[] {1, 2, 3, 0} );

        assertThat( channel.isOpen() )
            .isTrue();

        channel.close();

        assertThat( channel.isOpen() )
            .isFalse();
    }

    @Test
    public void shouldFailAfterClosed() throws IOException
    {
        ByteArrayInputStream is = new ByteArrayInputStream( new byte[] {1, 2, 3} );
        ReadableByteChannel channel = Channels.newChannel( is );
        channel.close();
        assertThat( channel.isOpen() ).isFalse();
        ee.expect( ClosedChannelException.class );
        channel.read( ByteBuffer.allocate( 0 ) );
    }

    @Test
    public void shouldFailIfNotReadable() throws IOException
    {
        ByteArrayInputStream is = new ByteArrayInputStream( new byte[] {1, 2, 3} );
        ReadableByteChannel channel = Channels.newChannel( is );
        ee.expect( NonReadableChannelException.class );
        channel.read( ByteBuffer.allocate( 0 ).asReadOnlyBuffer() );
    }

    @Test
    public void shouldFailIOnDirectBuffer() throws IOException
    {
        ByteArrayInputStream is = new ByteArrayInputStream( new byte[] {1, 2, 3} );
        ReadableByteChannel channel = Channels.newChannel( is );
        ee.expect( NonReadableChannelException.class );
        channel.read( ByteBuffer.allocateDirect( 0 ) );
    }

    @Test
    public void shouldUseFileChannel() throws IOException
    {
        File f = tmp.newFile();
        write( f.toPath(), new byte[] {1, 2, 3} );
        FileInputStream is = new FileInputStream( f );
        ReadableByteChannel channel = Channels.newChannel( is );
        ByteBuffer bb = ByteBuffer.allocate( 4 );
        int countWritten = channel.read( bb );

        assertThat( channel.isOpen() )
            .isTrue();

        channel.close();

        assertThat( channel.isOpen() )
            .isFalse();

        assertThat( countWritten )
            .isEqualTo( 3 );

        assertThat( bb.arrayOffset() )
            .isEqualTo( 0 );

        assertThat( bb.position() )
            .isEqualTo( 3 );

        assertThat( bb.remaining() )
            .isEqualTo( 1 );

        assertThat( bb.limit() )
            .isEqualTo( 4 );

        assertThat( bb.capacity() )
            .isEqualTo( 4 );

        bb.flip();

        assertThat( bb.arrayOffset() )
            .isEqualTo( 0 );

        assertThat( bb.position() )
            .isEqualTo( 0 );

        assertThat( bb.remaining() )
            .isEqualTo( 3 );

        assertThat( bb.limit() )
            .isEqualTo( 3 );

        assertThat( bb.capacity() )
            .isEqualTo( 4 );

        assertThat( bb.array() )
            .isEqualTo( new byte[] {1, 2, 3, 0} );
    }
}
