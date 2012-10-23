import com.sas.groovy.test.ThreadedTestCase;
import com.sas.groovy.util.Base64;
import java.net.URLEncoder;
import org.apache.log4j.Logger;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

/* The UrlPost class posts a series of urls given on the command line
 * or in a properties file.  In addition to the ThreadedTestCase and
 * TestCase properties, this class expects the following:
 *
 * -Decho:      When true (present) print the contents of each downloaded file to stdout
 *
 * -Durl:       The url to fetch.  Will be treated like url0.
 *
 * -DurlN:      The url to post.  Where N is an ever increasing, in order, integer
 *              starting at zero.  These urls will be posted in order.  url0, then
 *              url1, url2, etc.  Overrides -Durl.
 *
 * -Dusername:  The default username to use.  Basic Authentication support only.
 *
 * -DusernameN: The username to use when posting urlN.  Basic Authentication
 *              support only.
 *
 * -Dpassword:  The default password to use.  Basic Authentication support only.
 *
 * -DpasswordN: The username to use when posting urlN.  Basic Authentication
 *              support only.
 *
 * -Durlperthread: If true (present) then post only one url per thread based on
 *                 the thread number.  urlN will be posted on thread N.  If there
 *                 is no urlN specified then the thread will silently exit.  Otherwise
 *                 every thread will post every url specified.
 *
 * -Drequest:   The default path to a file containing the contents to post.  This can be a
 *              SOAP envelope or any contents.
 *
 * -DrequestN:  The path to a file containing the contents to post to urlN.
 *
 * -DcontentType: The default content of the contents to be posted.  Defaults to "text/xml; charset=utf-8".
 *
 * -DcontentType: the type of the contents to be posted to urlN.
 *
 * -Daction:    The default action to set as the SOAPAction header (not required).
 *
 * -DactionN:   The action to set as the SOAPAction header when posting to urlN.
 *
 * -Dstyle:     The default path to a local XSL file. The contents of this file will
 *              be used to transform the results of the post. Optional.
 *
 * -DstyleN:    The path to a local XSL file. The contents of this file will be used
 *              to transform the results of the post of urlN. Optional.
 *
 * -Doutfile:   The default path to a local file. This file is where the results of
 *              the post will be written. Optional.  Note that "${i}" in this string
 *              will be replaced with the current iteration id and "${t}" in this string
 *              will be replaced with the current thread id.
 *
 * -DoutfileN:  The path to a local file. This file is where the results of the post
 *              of urlN will be written. Optional.  Note that "${i}" in this string
 *              will be replaced with the current iteration id and "${t}" in this string
 *              will be replaced with the current thread id.
 *
 * Example command lines
 *
 * Post the SAS homepage and echo it to stdout.
 * runvjrscript -Drequest=request.xml -Decho -Durl0=http://www.sas.com urlPost.groovy
 *
 * Post the SAS homepage and then the CNN home page and echo them to stdout on 4 threads.
 * runvjrscript -Drequest=request.xml -Decho -Durl0=http://www.sas.com -Durl1=http://www.cnn.com -Dthread.count=4 urlPost.groovy
 */

class UrlPost extends ThreadedTestCase {

    Logger logger = Logger.getLogger(appLogger.getName() + ".UrlPost");
    Boolean echo         = true;
    String url           = null;
    Boolean urlperthread = false;
    String username      = "";
    String password      = "";
    String request       = "";
    String contentType   = "";
    String action        = "";
    String style         = "";
    String outfile       = "";

    void printHelp() {
        super.printHelp();
        println """
=============Test Properties=============

echo
When true (present) print the contents of each downloaded file to stdout

url
The url to fetch.  Will be treated like url0.

urlN
The url to post.  Where N is an ever increasing, in order, integer starting at zero.  These urls will be posted in order.  url0, then url1, url2, etc.  Overrides -Durl.

username
The default username to use for all requests.  Only used if there is not a usernameN to match the current urlN being posted.  Basic Authentication support only.

usernameN
The username to use when posting urlN.  Basic Authentication support only.

password
The default password to use for all requests.  Only used if there is not a passwordN to match the current urlN being posted.  Basic Authentication support only.

passwordN
The username to use when posting urlN.  Basic Authentication support only.

urlperthread
If true (present) then post only one url per thread based on the thread number.  urlN will be posted on thread N.  If there is no urlN specified then the thread will silently exit.  Otherwise every thread will post every url specified.

request
The default path to a file containing the contents to post.  This can be a SOAP envelope or any contents.

requestN
The path to a file containing the contents to post to urlN.

contentType
The default content of the contents to be posted.  Defaults to "text/xml; charset=utf-8".

contentTypeN
The type of the contents to be posted to urlN.

action
The default action to set as the SOAPAction header (not required).

actionN
The action to set as the SOAPAction header when posting to urlN.

style
The default path to a local XSL file. The contents of this file will be used to transform the results of the post. Optional.

styleN
The path to a local XSL file. The contents of this file will be used to transform the results of the post of urlN. Optional.

outfile
The default path to a local file. This file is where the results of the post will be written. Optional.  Note that "${i}" in this string will be replaced with the current iteration id and "${t}" in this string will be replaced with the current thread id.

outfileN
The path to a local file. This file is where the results of the post of urlN will be written. Optional.  Note that "${i}" in this string will be replaced with the current iteration id and "${t}" in this string will be replaced with the current thread id.""";
     }

    void setUp() {
        Properties p = getProperties();
        /* Check for any properties that can apply to any and all threads */
        url          = p.'url';
        urlperthread = p.containsKey( "urlperthread" );
        echo         = p.containsKey( "echo" );
        username     = p.'username';
        password     = p.'password';
        request      = p.'request';
        contentType  = p.getProperty( "contentType", "text/xml;charset=UTF-8");
        action       = p.'action';
        style        = p.'style';
        outfile      = p.'outfile';
        super.setUp();
    }

    void testUrlPost() {
        Properties p = getProperties();
        onExecutions { iteration, thread ->
            int i = 0;
            if( urlperthread ) {
                i = thread;
            }
            while(1) {
                /* Check for any thread specific properties */
                String urlString     = p.getProperty( "url${i}", url );
                String userString    = p.getProperty( "username${i}", username );
                String passString    = p.getProperty( "password${i}", password );
                String requestString = p.getProperty( "request${i}", request );
                String typeString    = p.getProperty( "contentType${i}", contentType );
                String actionString  = p.getProperty( "action${i}", action );
                String styleString   = p.getProperty( "style${i}", style );
                String outfileString = p.getProperty( "outfile${i}", outfile );
                ++i;
                if( !urlString || !request ) {
                    break;
                }

                /* Read the file to POST from disk */
                byte[] contents = readRequest( requestString, p );
                if( echo )
                    logger.info( "Posting: " + p.'line.separator' + new String(contents) );

                /* Open a connection to the requested url */
                URL postUrl = new URL(urlString);
                HttpURLConnection con = postUrl.openConnection();

                /* Set the POST properties */
                con.setRequestProperty( "Content-Length", String.valueOf( contents.length ) );
                con.setRequestProperty( "Content-Type", typeString );
                con.setRequestMethod( "POST" );
                con.setDoOutput( true );
                con.setDoInput( true );
                if( userString ) {
                    con.setRequestProperty( "Authorization", "Basic " + URLEncoder.encode( Base64.encode( userString + ":" + passString ), "UTF-8" ) );
                }
                if( actionString ) {
                    con.setRequestProperty( "SOAPAction", actionString );
                }

                /* Write the POST contents */
                OutputStream netOut = con.getOutputStream();
                netOut.write( contents );
                netOut.close();

                logger.info( "$urlString => ${con.getResponseCode()} ${con.getResponseMessage()}" );

                /* Open the output file if there is one */
                def out = null;
                if( outfileString ) {
                    outfileString = outfileString.replaceAll( /\$\{i\}/, iteration.toString() );
                    outfileString = outfileString.replaceAll( /\$\{t\}/, thread.toString() );
                    /* append if we are not on the first url */
                    out = new PrintStream( new FileOutputStream( outfileString, (i > 1) ) );
                }

                try {
                    /* Read the response content from the server */
                    BufferedReader br = new BufferedReader( new InputStreamReader( con.getInputStream() ) );
                    String result = br.text;
                    
                    if( echo ) {
                        logger.info( "Raw server response:" + p.'line.separator' + result );
                    }

                    /* If a stylesheet was given then transform the response */
                    if( styleString ) {
                        StringWriter styledResult = new StringWriter();
                        TransformerFactory tFactory = TransformerFactory.newInstance();
                        Transformer transformer = tFactory.newTransformer( new StreamSource( new FileReader( styleString ) ) );
                        transformer.transform(new StreamSource(new StringReader( result ) ), new StreamResult( styledResult ));
                        result = styledResult.toString();
                        /* If we are echoing then write the styled result to our logger */
                        if( echo ) {
                            logger.info( "Styled server response:" + p.'line.separator' + result );
                        }
                    }
                    /* If we have a file to write then write to it */
                    if( out ) {
                        out.println( result );
                    }
                } catch( Throwable t ) { /* ignore */ }
                /*
                 * If this is urlperthread then we have posted our
                 * one url and are done.  If we just had -Durl and
                 * didn't have any url0, url1, url2, etc then we are
                 * done.  Otherwise keep looping.
                 */
                if( urlperthread || url ) {
                    println "breaking";
                    break;
                }
            }
        }
    }

    public static byte[] readRequest( String request, Properties p ) throws IOException {
        String source = new File(request).text;
        /* Substitute any properties we can find now.*/
        p.each{ k, v ->
            if( k.startsWith("_") && k.endsWith("_") ) {
                source = source.replaceAll( k ) { return v; }
            }
        }
        return source.getBytes();
    }

}
