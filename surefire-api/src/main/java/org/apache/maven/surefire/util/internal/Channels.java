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

import javax.annotation.Nonnull;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

import static java.util.Objects.requireNonNull;

/**
 * Converts {@link OutputStream}, {@link java.io.PrintStream}, {@link InputStream}
 * to the Java {@link java.nio.channels.Channel}.
 * <br>
 * We do not use the Java's utility class {@link java.nio.channels.Channels} because the utility
 * closes the stream as soon as the particular Thread is interrupted.
 * If the frameworks (Zookeeper, Netty) interrupts the thread, the communication channels become
 * closed and the JVM hangs. Therefore we developed internal utility which is safe for the Surefire.
 *
 * @since 3.0.0-M5
 */
public final class Channels
{
    private Channels()
    {
        throw new IllegalStateException( "no instantiable constructor" );
    }

    public static WritableByteChannel newChannel( @Nonnull  OutputStream out )
    {
        return newChannel( out, false );
    }

    public static WritableByteChannel newFlushableChannel( @Nonnull OutputStream out )
    {
        return newChannel( out, true );
    }

    public static ReadableByteChannel newChannel( @Nonnull final InputStream is )
    {
        requireNonNull( is, "the stream should not be null" );

        if ( is instanceof FileInputStream && FileInputStream.class.equals( is.getClass() ) )
        {
            return ( (FileInputStream) is ).getChannel();
        }

        return new AbstractNoninterruptibleReadableChannel()
        {
            @Override
            protected int readImpl( ByteBuffer src ) throws IOException
            {
                int count = is.read( src.array(), src.arrayOffset() + src.position(), src.remaining() );
                if ( count > 0 )
                {
                    src.position( count + src.position() );
                }
                return count;
            }

            @Override
            protected void closeImpl() throws IOException
            {
                is.close();
            }
        };
    }

    private static WritableByteChannel newChannel( @Nonnull final OutputStream out, final boolean flushable )
    {
        requireNonNull( out, "the stream should not be null" );

        if ( out instanceof FileOutputStream && FileOutputStream.class.equals( out.getClass() ) )
        {
            return ( (FileOutputStream) out ).getChannel();
        }

        return new AbstractNoninterruptibleWritableChannel( flushable )
        {
            @Override
            protected void writeImpl( ByteBuffer src ) throws IOException
            {
                out.write( src.array(), src.arrayOffset() + src.position(), src.remaining() );
            }

            @Override
            protected void closeImpl() throws IOException
            {
                out.close();
            }

            @Override
            public void flush() throws IOException
            {
                out.flush();
            }
        };
    }
}
