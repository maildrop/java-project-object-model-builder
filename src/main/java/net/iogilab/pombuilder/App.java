package net.iogilab.pombuilder;

import java.lang.*;
import java.io.IOException;
import java.nio.file.*;
import org.w3c.dom.*;
import org.w3c.dom.ls.*;
import javax.xml.XMLConstants;
import javax.xml.xpath.*;

// net.iogilab.pombuilder.App
public final class App{
    private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger("net.iogilab.pombuilder.App");
    public static final class FileExistsException extends Exception{
    }

    public static void rewriteDocument( final org.w3c.dom.Document document , final java.util.Properties prop ){
        final XPathFactory factory;
        synchronized( XPathFactory.class ){
            factory = XPathFactory.newInstance();
        }
        synchronized( factory ){
            final XPath xpath = factory.newXPath();
            synchronized( xpath ){
                xpath.setNamespaceContext( new javax.xml.namespace.NamespaceContext(){
                    public String getNamespaceURI(final String prefix){
                        switch( prefix ){
                        case "maven":
                            return "http://maven.apache.org/POM/4.0.0";
                        case "xsi":
                            return "http://www.w3.org/2001/XMLSchema-instance";
                        case XMLConstants.XML_NS_PREFIX:
                            return XMLConstants.XML_NS_URI;
                        case XMLConstants.XMLNS_ATTRIBUTE:
                            return XMLConstants.XMLNS_ATTRIBUTE_NS_URI;
                        case XMLConstants.DEFAULT_NS_PREFIX:
                        default:
                            return XMLConstants.NULL_NS_URI;
                        }
                    }
                    public String getPrefix(final String namespaceURI){
                        switch( namespaceURI ){
                        case "http://maven.apache.org/POM/4.0.0":
                            return "maven";
                        case "http://www.w3.org/2001/XMLSchema-instance":
                            return "xsi";
                        case XMLConstants.XML_NS_URI:
                            return XMLConstants.XML_NS_PREFIX;
                        case XMLConstants.XMLNS_ATTRIBUTE_NS_URI:
                            return XMLConstants.XMLNS_ATTRIBUTE;
                        case XMLConstants.NULL_NS_URI:
                            return XMLConstants.DEFAULT_NS_PREFIX;
                        default:
                            return null;
                        }
                    }
                    public java.util.Iterator getPrefixes(final String namespaceURI ){
                        if( namespaceURI == null ){ throw new IllegalArgumentException(); }
                        switch(namespaceURI ){
                        case "http://maven.apache.org/POM/4.0.0":
                            return java.util.Arrays.asList( "maven" ).iterator();
                        case XMLConstants.XML_NS_URI:
                            return java.util.Arrays.asList( XMLConstants.XML_NS_PREFIX ).iterator();
                        case XMLConstants.XMLNS_ATTRIBUTE_NS_URI:
                            return java.util.Arrays.asList( XMLConstants.XMLNS_ATTRIBUTE ).iterator();
                        default:
                            return new java.util.Iterator(){
                                public boolean hasNext(){ return false; }
                                public Object next(){ throw new java.util.NoSuchElementException(); }
                            };
                        }
                    }
                    } );

                try{
                    final Node node = (Node)xpath.evaluate( "/maven:project/maven:artifactId",document,XPathConstants.NODE );
                    final Element element = (Element)node;
                    element.setTextContent( String.class.cast(prop.get("artifactId") ));
                }catch( final XPathExpressionException xpee ){
                    logger.severe( xpee.toString() );
                }
                
            }
        }
    }
    
    public static void doGenerate( final java.io.Writer writer , final java.util.Properties prop )
        throws IOException{
        
        try{
            final org.w3c.dom.bootstrap.DOMImplementationRegistry reg = org.w3c.dom.bootstrap.DOMImplementationRegistry.newInstance();
            final DOMImplementation impl;
            synchronized( reg ){
                impl = reg.getDOMImplementation( "XML 3.0 +LS 3.0");
            }
            final Document document = impl.createDocument("http://maven.apache.org/POM/4.0.0", "project", null );
            final DOMImplementationLS ls = (DOMImplementationLS)document.getImplementation().getFeature( "LS" , "3.0" );

            // import template;
            try(final java.io.InputStream is = App.class.getResourceAsStream("/net/iogilab/pombuilder/template/pom.xml")){
                final LSInput lsinput = ls.createLSInput();
                lsinput.setByteStream( is );
                final LSParser parser = ls.createLSParser( DOMImplementationLS.MODE_SYNCHRONOUS, null);
                final Document importDocument = parser.parse( lsinput );

                final Element documentNode = importDocument.getDocumentElement();
                final NodeList childNodes = documentNode.getChildNodes();
                for( int i = 0 ; i< childNodes.getLength() ; ++i ){
                    document.getDocumentElement().appendChild( document.importNode( childNodes.item(i) , true ) );
                }
            }

            rewriteDocument( document , prop );
            
            final LSOutput lsoutput = ls.createLSOutput();
            lsoutput.setEncoding( "UTF-8" );
            lsoutput.setCharacterStream( writer );
            ls.createLSSerializer().write( document , lsoutput );
        }catch(final ClassNotFoundException cnfe ){
            throw new IOException( cnfe );
        }catch(final InstantiationException ie ){
            throw new IOException( ie );
        }catch(final IllegalAccessException iae ){
            throw new IOException( iae );
        }
        return;
    }
    
    public static final void main(final String args[]){
        final org.apache.commons.cli.Options options =
            new org.apache.commons.cli.Options()
            .addOption("a",true,"artifactId")
            .addOption("o",true,"output file")
            .addOption("h",false,"help");
        try{
            final org.apache.commons.cli.CommandLine cmd =
                new org.apache.commons.cli.DefaultParser().parse( options, args );
            if( cmd.hasOption("h") ){
                new org.apache.commons.cli.HelpFormatter().printHelp( "pombuilder", options );
                return ;
            }
            final java.util.Properties prop = new java.util.Properties();
            final java.io.Writer output;
            if( cmd.hasOption("o") ){
                final Path path = FileSystems.getDefault().getPath( cmd.getOptionValue( "o" )  );
                final java.io.File baseDirectory;
                if( path.getParent() != null ) {
                    baseDirectory = path.getParent().toFile();
                }else{
                    baseDirectory = FileSystems.getDefault().getPath(".").toFile();
                }
                logger.info( baseDirectory.toString());
                for( final String p : new String[]{
                        "src",
                        "src/main",
                        "src/test",
                        "src/main/java",
                        "src/main/resources",
                        "src/test/java",
                        "src/test/resources"
                    } ){
                    final java.io.File f = new java.io.File( baseDirectory , p );
                    f.mkdir();
                }

                final java.io.File file = path.toFile();
                if( file.exists() ){
                    throw new FileExistsException();
                }
                
                output = Files.newBufferedWriter( path , java.nio.charset.StandardCharsets.UTF_8 );
            }else{
                output = new java.io.OutputStreamWriter(System.out);
            }
            if( cmd.hasOption("a") ){
                prop.put("artifactId" , cmd.getOptionValue("a"));
            }else{
                prop.put("artifactId" , "com.example" );
            }
            try{
                doGenerate( output , prop );
            }finally{
                output.close();
            }
        }catch( final FileExistsException fee ){
            System.err.println( fee.toString() );
        }catch( final InvalidPathException ipe ){
            System.err.println( ipe.toString());
        }catch( final org.apache.commons.cli.ParseException pe ){
            System.err.println( pe.toString());
        }catch( final java.io.IOException ioe ){
            System.err.println( ioe.toString() );
        }
        return;
    }
}
