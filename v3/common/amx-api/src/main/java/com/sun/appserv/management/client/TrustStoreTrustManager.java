/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright 1997-2007 Sun Microsystems, Inc. All rights reserved.
 * 
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License. You can obtain
 * a copy of the License at https://glassfish.dev.java.net/public/CDDL+GPL.html
 * or glassfish/bootstrap/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 * 
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at glassfish/bootstrap/legal/LICENSE.txt.
 * Sun designates this particular file as subject to the "Classpath" exception
 * as provided by Sun in the GPL Version 2 section of the License file that
 * accompanied this code.  If applicable, add the following below the License
 * Header, with the fields enclosed by brackets [] replaced by your own
 * identifying information: "Portions Copyrighted [year]
 * [name of copyright owner]"
 * 
 * Contributor(s):
 * 
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */
package com.sun.appserv.management.client;

import com.sun.appserv.management.util.misc.LineReaderImpl;

import javax.net.ssl.X509TrustManager;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.text.DateFormat;
import java.util.Date;


/**
	This X509TrustManager implementation supports a trust-store file and allows
	adding new certificates to it.  It is designed to allow a subclass to
	override a variety of protected methods including those of TrustManager:
	<ul>
	<li>checkClientTrusted</li>
	<li>checkServerTrusted</li>
	<li>getAcceptedIssuers</li>
	</ul>
	
	as well as:
	
	<ul>
	<li>#checkCertificate</li>
	<li>#getTrustStorePassword</li>
	<li>#shouldAddToTrustStore</li>
	<li>#askShouldAddToTrustStore</li>
	<li>#getCertificateAlias</li>
	<li>#addCertificateToTrustStore</li>
	<li>#writeStore</li>
	<li>#certificateNotInTrustStore</li>
	<li>#getTrustStore</li>
	</ul>
	<p>
	For convenience, if setPrompt( true ) is called, then when a new Certificate
	is encountered, askShouldAddToTrustStore( c ) prompts the user
	via System.in as to whether to accept this new Certificate as trusted.
	Subclasses can of course override this behavior any any desired way.
 */
public class TrustStoreTrustManager
	implements X509TrustManager // do NOT make Serializable
{
	private final File		mTrustStoreFile;
	private final char[]	mTrustStorePassword;
	private final String	mKeyStoreType;
	private KeyStore		mTrustStore;
	private boolean			mPrompt;
	
	/**
		Create a new instance with the specified File and password
		The trustStoreFile must exist.
		
		@param trustStoreFile		(not required to exist)
		@param keyStoreType		keystore (truststore) type, eg "JKS"
		@param trustStorePassword (may be null)
	 */
		public
	TrustStoreTrustManager(
		final File		trustStoreFile,
		final String	keyStoreType,
		final char[]	trustStorePassword )
	{
		if ( trustStoreFile == null || keyStoreType == null )
		{
			throw new IllegalArgumentException();
		}
		
		mTrustStoreFile		= trustStoreFile;
		mKeyStoreType		= keyStoreType;
		mTrustStorePassword	= trustStorePassword;
		mTrustStore			= null;
		mPrompt				= false;
		
		try
		{
			getTrustStore();	// force initialization now
		}
		catch( Exception e )
		{
			throw new RuntimeException( e );
		}
	}
	
	/**
		calls this( trustStoreFile,"JKS", trustStorePassword )
	 */
		public
	TrustStoreTrustManager(
		final File		trustStoreFile,
		final char[]	trustStorePassword )
	{
		this( trustStoreFile, "JKS", trustStorePassword );
	}
	
	/**
		If set to true, then when a new Certificate is encountered, the user
		will be prompted via System.in as to whether it should be trusted.
		
		@param prompt
	 */
		public void
	setPrompt( final boolean prompt )
	{
		mPrompt	= prompt;
	}
	
	/**
		Create an instance using the system trust-store as returned by 
		getSystemTrustStoreFile(). 
		
		@return an instance or null if not possible
	 */
		public static TrustStoreTrustManager
	getSystemInstance()
	{
    	final File		trustStore			= getSystemTrustStoreFile();
    	final char[]	trustStorePassword	= getSystemTrustStorePassword();
    	
    	TrustStoreTrustManager	mgr	= null;
    	
    	if ( trustStore != null && trustStorePassword != null )
    	{
    		return( new TrustStoreTrustManager( trustStore, trustStorePassword ) );
    	}
    	
    	return( mgr );
	}
	
		private static char[]
	toCharArray( final String s )
	{
		return( s == null ? null : s.toCharArray() );
	}
	
	
	/**
		Standard system property denoting the trust-store.
	 */
	public static final String	TRUSTSTORE_FILE_SPROP	= "javax.net.ssl.trustStore";
	
	/**
		Standard system property denoting the trust-store password.
	 */
	public static final String	TRUSTSTORE_PASSWORD_SPROP= "javax.net.ssl.trustStorePassword";
	
	/**
		Use System.getProperty( "javax.net.ssl.trustStore" ) to find a trust-store.
	 */
		public static File
	getSystemTrustStoreFile()
	{
		final String	prop	= System.getProperty( TRUSTSTORE_FILE_SPROP );
		final File trustStore	= prop == null ? null : new File( prop );
		return( trustStore );
	}
	
	/**
		Use System.getProperty( "javax.net.ssl.trustStorePassword" ) to find the
		trust-store password.
	 */
		public static char[]
	getSystemTrustStorePassword()
	{
		return( toCharArray( System.getProperty( TRUSTSTORE_PASSWORD_SPROP ) ) );
	}
    
	
	/**
		Return the trust-store that was initially passed in.
		
		@return File
	 */
		public final File
	getTrustStoreFile()
	{
		return( mTrustStoreFile );
	}
	
	/**
		Subclass may choose to override this method to get the password from any
		desired source.  Otherwise, the password used to create this instance is
		returned.
		
		@return char[]
	 */
		protected char[]
	getTrustStorePassword()
	{
		return( mTrustStorePassword );
	}
	
		public void
	checkClientTrusted( X509Certificate[] chain, String authType)
		throws CertificateException
	{
		throw new UnsupportedOperationException( "checkClientTrusted() not supported" );
	}
	
		public void
	checkServerTrusted( X509Certificate[] chain, String authType)
		throws CertificateException
	{
		if (chain == null || chain.length == 0)
		{
			throw new IllegalArgumentException();
        }
        
		checkCertificate(chain);
	}
	
	/**
		By default, no issuers are trusted. It is better to trust specific 
		Certificates explicitly.
		
		@return X509Certificate[]
	 */
		public X509Certificate[]
	getAcceptedIssuers()
	{
		// none, by default
		return( new X509Certificate[ 0 ] );
	}
	
	/**
		Prompts via System.in to ask whether the Certificate should be added.
		
		@param c
		@return true if the response is yes.
	 */
		protected boolean
	askShouldAddToTrustStore( final Certificate c )
		throws IOException
	{
		final LineReaderImpl	reader	= new LineReaderImpl( System.in );
		
		final String prompt	= c.toString() + 
			"\n\nAdd the above certificate to the truststore [y/n]?";
			
		final String result	= reader.readLine( prompt );
		
		return( result.equalsIgnoreCase( "y" ) || result.equalsIgnoreCase( "yes" ) );
	}
	
	/**
		Subclass may wish to override this routine and call defaultShouldAddToTrustStore( c );
		
		@param c
		@return true if the Certificate should be trusted and added to the trust-store
	 */
		protected boolean
	shouldAddToTrustStore( final Certificate c )
		throws IOException
	{
		return( mPrompt ? askShouldAddToTrustStore( c ) : false );
	}
	
	/**
		Return an alias for a Certificate to be added to the TrustStore.
		@param c
		@return an alias to be used for adding the Certificate to the trust-store
	 */
		protected String
	getCertificateAlias( final Certificate c )
	{
        final DateFormat f = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.LONG);     
        
		return( "cert" +  f.format( new Date() ) );
	}
	
	
	/**
		Add the Certificate with the specified alias to the trust-store.
		
		@param alias
		@param c
	 */
		protected void
	addCertificateToTrustStore(
		final String		alias,
		final Certificate	c )
		throws IOException,
		KeyStoreException, NoSuchAlgorithmException, CertificateException
	{
        mTrustStore.setCertificateEntry( alias, c );
       	writeStore();
	}
	
	
	/**
		Add the Certificate to the trust-store, using the alias returned by
		getCertificateAlias( c ).
		
		@param c
	 */
		protected void
	addCertificateToTrustStore( final Certificate c )
		throws IOException,
		KeyStoreException, NoSuchAlgorithmException, CertificateException
	{
        final String aliasName = getCertificateAlias( c );
        
        addCertificateToTrustStore( aliasName, c );
	}
	
		private void
	writeStore(
		final KeyStore	trustStore,
		final char[]	trustStorePassword,
		final File		f )
		throws IOException,
		KeyStoreException, NoSuchAlgorithmException, CertificateException
	{	
		FileOutputStream	out	= new FileOutputStream( f );
    	
		try
		{
			trustStore.store( out, trustStorePassword );
		}
		catch( Throwable t )
		{
			t.printStackTrace();
		}
		finally
		{
			out.close();
		}
    }    
    
	/**
		Write the store to disk.  Results are undefined if an error occurs while
		writing the file.
	 */
		protected void
	writeStore()
		throws IOException,
		KeyStoreException, NoSuchAlgorithmException, CertificateException
	{	
		writeStore( getTrustStore(), getTrustStorePassword(), getTrustStoreFile() );
    	// NOTE: any exception thrown from here is squelched by calling JDK code
    	// if in the middle of a SSL negotiation
    }    
    
	
	/**
		The Certificate is not found in the trust-store.
		If shouldAddToTrustStore( c ) returns false, then a CertificateException
		is thrown.  Otherwise, addCertificateToTrustStore( c ) is called.
		
		@param c
	 */
		protected void
	certificateNotInTrustStore( final Certificate c )
		throws IOException,
		KeyStoreException, NoSuchAlgorithmException, CertificateException
	{
        if ( shouldAddToTrustStore( c ) )
        {
        	addCertificateToTrustStore( c );
        }
        else
        {
            throw new CertificateException( "Certificate not trusted:\n" + c );
        }
	}
	
		private void
	createTrustStoreFile(
		final KeyStore	keyStore,
		final char[]	pw,
		final File 		f )
		throws IOException,
			CertificateException, NoSuchAlgorithmException,
			KeyStoreException, FileNotFoundException
	{
		f.createNewFile();
		writeStore( keyStore, pw, f );
	}

	/**
		Get the KeyStore containing the Certificates to be trusted.  This should
		be a KeyStore corresponding to the file that was specified.  The same
		KeyStore should be returned each time.
		
		@return KeyStore
	 */
		protected synchronized KeyStore
	getTrustStore()
		throws IOException,
			CertificateException, NoSuchAlgorithmException, KeyStoreException, FileNotFoundException
	{
		if ( mTrustStore == null )
		{
			mTrustStore	= KeyStore.getInstance( mKeyStoreType );
			final File	f	= getTrustStoreFile();
			final char[]	pw	= getTrustStorePassword();
			if ( (! f.exists()) || f.length() == 0 )
			{
				f.delete();
				mTrustStore.load( null, pw );
				createTrustStoreFile( mTrustStore, pw, f);
			}
			else
			{
				final FileInputStream is	= new FileInputStream( f );
				try
				{
					mTrustStore.load( is, pw );
				}
				finally
				{
					is.close();
				}
			}
		}
		
		return( mTrustStore );
	}
	
	/**
     	@param chain
     	@throws RuntimeException
     	@throws CertificateException
     */    
		protected void
	checkCertificate( final X509Certificate[] chain)
		throws RuntimeException, CertificateException
    {
		try
		{
            //First ensure that the certificate is valid.
            for (int i = 0 ; i < chain.length ; i ++)
            {
                chain[i].checkValidity();   
            }
            
            mTrustStore	= getTrustStore();
            
            final Certificate	cert	= chain[ 0 ];
            
            //if the certificate already exists in the truststore, it is implicitly trusted
            if ( mTrustStore.getCertificateAlias( cert ) == null )
            {
            	certificateNotInTrustStore( cert );
            }
        }
        catch (CertificateException e)
        {
            throw e;
        }
        catch (Exception e)
        {        
			throw new RuntimeException( e );
		}
	}

	
		public String
	toString()
	{
		return( "TrustStoreTrustManager--trusts certificates found in truststore: " + mTrustStore );
	}
}



