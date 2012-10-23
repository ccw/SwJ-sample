package data;

import com.sas.metadata.remote.*;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 *
 */
public class SasMetaInterface {

    private static String host = "172.17.64.177";  // metadata server host
    private static String port = "8561";       // metadata server port
    private static String user = "sas_ken@saspw";     // user name for metadata server
    private static String pass = "kKk123";     // password for metadata server
    private static String rpos = "Foundation"; // repository

    private MdFactory _factory = null;

    public SasMetaInterface() {
        initializeFactory();
        connectToServer();
    }

    private void initializeFactory() {
        try {
            _factory = new MdFactoryImpl(false);
            _factory.setDebug(false);
            _factory.setLoggingEnabled(false);
            _factory.getUtil().setOutputStream(System.out);
            _factory.getUtil().setLogStream(System.out);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * The following statements define variables for SAS Metadata Server
     * connection properties, instantiate a connection factory, issue
     * the makeOMRConnection method, and check exceptions for error conditions.
     */
    public boolean connectToServer() {
        try {
            MdOMRConnection connection = _factory.getConnection();
            connection.makeOMRConnection(
                    host,
                    port,
                    user,
                    pass
            );

        } catch (MdException e) {
            Throwable t = e.getCause();
            if (t != null) {
                String ErrorType = e.getSASMessageSeverity();
                String ErrorMsg = e.getSASMessage();
                if (ErrorType == null) {
                } else {
                    System.out.println(ErrorType + ": " + ErrorMsg);
                }
                if (t instanceof org.omg.CORBA.COMM_FAILURE) {
                    System.out.println(e.getLocalizedMessage());
                } else if (t instanceof org.omg.CORBA.NO_PERMISSION) {
                    System.out.println(e.getLocalizedMessage());
                }
            } else {
                System.out.println(e.getLocalizedMessage());
            }
            e.printStackTrace();
            return false;
        } catch (RemoteException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public ArrayList getMetadataTypes() {
        try {
            // Metadata types are listed with the getTypes method.
            System.out.println("\nThe object types contained on this server are: ");
            List nameList = new ArrayList(100);
            List descList = new ArrayList(100);
            _factory.getOMIUtil().getTypes(nameList, descList);
            Iterator iter1 = nameList.iterator();
            Iterator iter2 = descList.iterator();
            while (iter1.hasNext() && iter2.hasNext()) {
                // Print the name and description of each metadata object type
                String name = (String) iter1.next();
                String desc = (String) iter2.next();
                System.out.println("Type: " + name + ", desc: " + desc);
            }
            return (ArrayList) nameList;
        } catch (MdException e) {
            e.printStackTrace();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * This example retrieves a list of the repositories that are registered
     * on the SAS Metadata Server.
     *
     * @return the list of available repositories (list of CMetadata objects)
     */
    public CMetadata getRepository(String name) {
        CMetadata repository = null;
        try {
            System.out.println("\nThe repositories contained on this server are: ");

            MdOMIUtil omiUtil = _factory.getOMIUtil();
            List reposList = omiUtil.getRepositories();
            Iterator iter = reposList.iterator();
            while (iter.hasNext()) {
                CMetadata r = (CMetadata) iter.next();
                System.out.println("\t" + r.getName());
                if (r.getName().equalsIgnoreCase(name)) repository = r;
            }
        } catch (MdException e) {
            e.printStackTrace();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return repository;
    }

    /**
     * This example displays the PhysicalTable objects in a repository.
     *
     * @param repository CMetadata identifies the repository from which to read
     *                   the objects.
     */
    public void displayAllObjects(CMetadata repository, String objectType) {
        try {
            System.out.println("\nRetrieving all " + objectType + " objects contained in "
                    + " repository " + repository.getName());
            String reposID = repository.getFQID();
            MdObjectStore store = _factory.createObjectStore();
            int flags = MdOMIUtil.OMI_GET_METADATA | MdOMIUtil.OMI_ALL_SIMPLE;
            List objects = _factory.getOMIUtil().getMetadataObjectsSubset(
                    store,
                    reposID, // Repository to search
                    objectType, // Metadata type to search for
                    flags,
                    ""
            );
            System.out.println("Found " + objects.size() + " " + objectType + "(s)");
            if (objects.size() > 0 && objects.size() <= 100) {
                displayObjectInfo(objects, objectType);
            }
            store.dispose();
        } catch (MdException e) {
            e.printStackTrace();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private void displayObjectInfo(List objects, String objectType) throws MdException, RemoteException {
        if (MetadataObjects.INTERNALLOGIN.equalsIgnoreCase(objectType)) {
            for (Object object : objects) {
                InternalLogin login = (InternalLogin) object;
                System.out.println("[Id: " + login.getId() + ", Name: " + login.getName() + ", Pass: " + login.getPasswordHash() + "]");
            }
        } else if (MetadataObjects.PERSON.equalsIgnoreCase(objectType)) {
            for (Object object : objects) {
                Person person = (Person) object;

                StringBuilder eiString = new StringBuilder();
                AssociationList eis = person.getExternalIdentities();
                for (Object eio : eis) {
                    ExternalIdentity ei = (ExternalIdentity) eio;
                    eiString.append("\n\t\t{Name: ").append(ei.getName()).append(", Type:").append(ei.getIdentifier()).append("}");
                }

                StringBuilder groupString = new StringBuilder();
                AssociationList igs = person.getIdentityGroups();
                for (Object igo : igs) {
                    IdentityGroup ig = (IdentityGroup) igo;
                    groupString.append("\n\t\t{Name: ").append(ig.getName()).append(", Type:").append(ig.getGroupType()).append("}");
                }
                System.out.println("[Id: " + person.getId() + ", Name: " + person.getName()+ "\n\tGroup: " + groupString + "\n\tExternalIdentity: " + eiString + "]");
            }
        }

    }

    /**
     * This example reads the newly created objects from the server.
     *
     * @param repository identifies the repository from which to read our objects.
     */
    public void readPerson(CMetadata repository, String identity) {
        if (repository != null) {
            try {
                System.out.println("\nReading objects from the server...");
                MdObjectStore store = _factory.createObjectStore();
                String xmlSelect = "<XMLSELECT Search=\"@Name='" + identity + "'\"/>";
                String template = "";

                String sOptions = xmlSelect + template;

                int flags = MdOMIUtil.OMI_XMLSELECT | MdOMIUtil.OMI_TEMPLATE |
                        MdOMIUtil.OMI_GET_METADATA;
                List tableList = _factory.getOMIUtil().getMetadataObjectsSubset(
                        store,
                        repository.getFQID(),
                        MetadataObjects.PERSON,
                        flags,
                        sOptions
                );
                Iterator iter = tableList.iterator();
                while (iter.hasNext()) {

                    Person user = (Person) iter.next();
                    System.out.println("Found person: " + user.getName() + " (" + user.getId() + ")");

                    InternalLogin iLogin = user.getInternalLoginInfo();
                    if (iLogin != null) {
                        System.out.println("\tInternal Login Info ->");
                        System.out.println("\tLogin: " + iLogin.getName() + " (" + iLogin.getId() + ")");
                        System.out.println("\tPassword: " + iLogin.getPasswordHash());
                        System.out.println("\tSalt: " + iLogin.getSalt());
                    }

                    AssociationList logins = user.getLogins();
                    for (int i = 0; i < logins.size(); i++) {
                        Login login = (Login) logins.get(i);
                        System.out.println("\tDomain: " + login.getDomain().getName() + " (" + login.getDomain().getId() + ")");
                        System.out.println("\tLogin: " + login.getName() + " (" + login.getId() + ")");
                        System.out.println("\tPassword: " + login.getPassword());
                    }

                    AssociationList groups = user.getIdentityGroups();
                    for (int i = 0; i < groups.size(); i++) {
                        IdentityGroup group = (IdentityGroup) groups.get(i);
                        System.out.println("\tGroup: " + group.getName() + " (" + group.getId() + ")");
                        System.out.println("\t\tPublicType: " + group.getPublicType());
                    }
                }
                store.dispose();
            } catch (MdException e) {
                e.printStackTrace();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) throws Exception {
        SasMetaInterface sas = new SasMetaInterface();
        //ArrayList metaTypes = sas.getMetadataTypes();

        CMetadata foundation = sas.getRepository(rpos);
        System.out.println("Foundation -> " + foundation);

//        for (int i = 0; i < metaTypes.size(); i++) {
//            sas.displayAllObjects(foundation, MetadataObjects.PERSON);
//        }

        sas.readPerson(foundation, "sas_ken");
    }

}
