/**
 *
 */

package org.odk.collect.onic.logic;

import org.javarosa.core.reference.PrefixedRootFactory;
import org.javarosa.core.reference.Reference;

/**
 * @author ctsims
 */
public class FileReferenceFactory extends PrefixedRootFactory {

    String localRoot;


    public FileReferenceFactory(String localRoot) {
        super(new String[]{
                "file"
        });
        this.localRoot = localRoot;
    }


    @Override
    protected Reference factory(String terminal, String uri) {
        return new FileReference(localRoot, terminal);
    }

}
