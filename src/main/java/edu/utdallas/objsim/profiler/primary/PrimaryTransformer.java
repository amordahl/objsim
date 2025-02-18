package edu.utdallas.objsim.profiler.primary;

/*
 * #%L
 * objsim
 * %%
 * Copyright (C) 2020 The University of Texas at Dallas
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import edu.utdallas.objsim.commons.asm.ComputeClassWriter;
import org.apache.commons.lang3.tuple.Pair;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.pitest.classinfo.ClassByteArraySource;

import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;
import java.util.HashMap;
import java.util.Map;
import java.nio.file.*;
import static edu.utdallas.objsim.commons.misc.NameUtils.decomposeMethodName;
import static org.pitest.bytecode.FrameOptions.pickFlags;

/**
 * A versatile class file transformer that adds code to record system state at the exit
 * point(s) of a patched method.
 * !Internal use only!
 *
 * @author Ali Ghanbari (ali.ghanbari@utdallas.edu)
 */
public class PrimaryTransformer implements ClassFileTransformer {
    private final ClassByteArraySource byteArraySource;

    private final Map<String, String> cache;

    private final String patchedClassName;

    private final String patchedMethodFullName;

    public PrimaryTransformer(final String patchedMethodFullName,
                              final ClassByteArraySource byteArraySource) {
        final int indexOfLP = patchedMethodFullName.indexOf('(');
        final Pair<String, String> methodNameParts =
                decomposeMethodName(patchedMethodFullName.substring(0, indexOfLP));
        this.patchedClassName = methodNameParts.getLeft().replace('.', '/');
        this.patchedMethodFullName = patchedMethodFullName;
        this.cache = new HashMap<>();
        this.byteArraySource = byteArraySource;
    }

	public PrimaryTransformer(final ClassByteArraySource byteArraySource) {
		this.cache = new HashMap<>();
		this.byteArraySource = byteArraySource;
		this.patchedClassName = null;
		this.patchedMethodFullName = null;
	}
	
    @Override
    public byte[] transform(ClassLoader loader,
                            String className,
                            Class<?> classBeingRedefined,
                            ProtectionDomain protectionDomain,
                            byte[] classfileBuffer) {
    	if (className.contains("utdallas")) {
    		System.out.println("Skipping class " + className);
    		return null;
    	}
    	System.out.println("[Primary Transformer] ClassName is " + className);
        final ClassReader classReader = new ClassReader(classfileBuffer);
        final ClassWriter classWriter = new ComputeClassWriter(this.byteArraySource, this.cache, pickFlags(classfileBuffer));
        final ClassVisitor classVisitor = new PrimaryTransformerClassVisitor(classfileBuffer, classWriter);
        classReader.accept(classVisitor, ClassReader.EXPAND_FRAMES);
        
        // This is to write the byte array to a file.
        try {
        	Path outputFolder = Paths.get("/Users/austin/Desktop/outputs");
        	if (!Files.exists(outputFolder, new LinkOption[] {})) {
        		Files.createDirectory(outputFolder);
        	}
        	Path outputFile = outputFolder.resolve(className.replace("/", "_") + ".class");
        	System.err.println("Outputfile is " + outputFile.toString());
        	Files.write(outputFile, classWriter.toByteArray());
        } catch (Exception ex) {
        	System.err.println(ex.getMessage());
        	ex.printStackTrace();
        }
        return classWriter.toByteArray();
    }
}
