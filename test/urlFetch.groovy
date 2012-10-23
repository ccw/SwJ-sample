import com.sas.groovy.test.ThreadedTestCase;
import com.sas.groovy.util.Base64;
import java.net.URLEncoder;
import org.apache.log4j.Logger;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;


/* The UrlFetch class fetches a series of urls given on the command line
 * or in a properties file.  In addition to the ThreadedTestCase and
 * TestCase properties, this class expects the following:
 *
 * -Decho:      When true (present) print the contents of each downloaded file to stdout
 *
 * -Durl:       The url to fetch.  Will be treated like url0.
 *
 * -DurlN:      The url to fetch.  Where N is an ever increasing, in order, integer
 *              starting at zero.  These urls will be fetched in order.  url0, then
 *              url1, url2, etc.  Overrides -Durl.
 *
 * -Dusername:  The username to use.  Basic Authentication support only.
 *
 * -DusernameN: The username to use when fetching urlN.  Basic Authentication
 *              support only.
 *
 * -Dpassword:  The password to use.  Basic Authentication support only.
 *
 * -DpasswordN: The username to use when fetching urlN.  Basic Authentication
 *              support only.
 *
 * -Durlperthread: If true (present) then fetch only one url per thread based on
 *                 the thread number.  urlN will be fetched on thread N.  If there
 *                 is no urlN specified then the thread will silently exit.  Otherwise
 *                 every thread will fetch every url specified.
 *
 * -Dstyle:     The default path to a local XSL file. The contents of this file will
 *              be used to transform the results of the fetch. Optional.
 * -DstyleN:    The path to a local XSL file. The contents of this file will be used
 *              to transform the results of the fetch of urlN. Optional.
 * -Doutfile:   The default path to a local file. This file is where the results of
 *              the fetch will be written. Optional.  Note that "${i}" in this string
 *              will be replaced with the current iteration id and "${t}" in this string
 *              will be replaced with the current thread id.
 *
 * -DoutfileN:  The path to a local file. This file is where the results of the fetch
 *              of urlN will be written. Optional.  Note that "${i}" in this string will
 *              be replaced with the current iteration id and "${t}" in this string will
 *              be replaced with the current thread id.
 *
 * Example command lines
 *
 * Fetch the SAS homepage and echo it to stdout.
 * runvjrscript -Decho -Durl0=http://www.sas.com urlFetch.groovy
 *
 * Fetch the SAS homepage and then the CNN home page and echo them to stdout on 4 threads.
 * runvjrscript -Decho -Durl0=http://www.sas.com -Durl1=http://www.cnn.com -Dthread.count=4 urlFetch.groovy
 */

class UrlFetch extends ThreadedTestCase {

    Logger logger = Logger.getLogger(appLogger.getName() + ".UrlFetch");
    Boolean echo         = true;
    String url           = null;
    Boolean urlperthread = false;
    String username      = "";
    String password      = "";
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
The url to fetch.  Where N is an ever increasing, in order, integer starting at zero.  These urls will be fetched in order.  url0, then url1, url2, etc.

username
The default username to use for all requests.  Only used if there is not a usernameN to match the current urlN being fetched.  Basic Authentication support only.

usernameN
The username to use when fetching urlN.  Basic Authentication support only.

password
The default password to use for all requests.  Only used if there is not a passwordN to match the current urlN being fetched.  Basic Authentication support only.

passwordN
The username to use when fetching urlN.  Basic Authentication support only.

urlperthread
If true (present) then fetch only one url per thread based on the thread number.  urlN will be fetched on thread N.  If there is no urlN specified then the thread will silently exit.  Otherwise every thread will fetch every url specified.

style
The default path to a local XSL file. The contents of this file will be used to transform the results of the fetch. Optional.

styleN
The path to a local XSL file. The contents of this file will be used to transform the results of the fetch of urlN. Optional.

outfile
The default path to a local file. This file is where the results of the fetch will be written. Optional.  Note that "${i}" in this string will be replaced with the current iteration id and "${t}" in this string will be replaced with the current thread id.

outfileN
The path to a local file. This file is where the results of the fetch of urlN will be written. Optional.  Note that "${i}" in this string will be replaced with the current iteration id and "${t}" in this string will be replaced with the current thread id.""";
    }

    void setUp() {
        Properties p = getProperties();
        /* Check for any properties that can apply to any and all threads */
        url             = p.'url';
        urlperthread    = p.containsKey( "urlperthread" );
        echo            = p.containsKey( "echo" );
        username        = p.'username';
        password        = p.'password';
        style           = p.'style';
        outfile         = p.'outfile';
        super.setUp();
    }

    void testUrlFetch() {
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
                String styleString   = p.getProperty( "style${i}", style );
                String outfileString = p.getProperty( "outfile${i}", outfile );
                ++i;
                if( !urlString ) {
                    break;
                }

                /* Open a connection to the requested url */
                URL fetchUrl = new URL(urlString);
                HttpURLConnection con = fetchUrl.openConnection();

                /* Set the authentication information if there is any */
                if( userString ) {
                    con.setRequestProperty( "Authorization", "Basic " + URLEncoder.encode( Base64.encode( userString + ":" + passString ), "UTF-8" ) );
                }
                logger.info( "$urlString => ${con.getResponseCode()} ${con.getResponseMessage()}" );

                /* Open the output file if there is one */
                def out = null;
                if( outfileString ) {
                    outfileString = outfileString.replaceAll( /\$\{i\}/, iteration.toString() );
                    outfileString = outfileString.replaceAll( /\$\{t\}/, thread.toString() );
                    out = new PrintStream( new FileOutputStream( outfileString, i > 1 ) );
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
                } catch( Throwable t ) {
                    logger.error( t.toString() );
                }
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
}
