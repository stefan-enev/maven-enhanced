package com.ebay.git.commands;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;
import java.util.SortedMap;

import org.eclipse.jgit.lib.AnyObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.RefComparator;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.pgm.TextBuiltin;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.util.RefMap;

import com.ebay.maven.binaryrepository.GitException;

public class ShowRef extends TextBuiltin {
	
	private Repository repository;
	
	public ShowRef( Repository repository ){
		this.repository = repository;
		this.out = new PrintWriter(System.out);
	}
	
	public ShowRef( File root) throws GitException{
		this( root, new PrintWriter(System.out));
	}
	
	public ShowRef( File root, PrintWriter out ) throws GitException{
		FileRepositoryBuilder repobuiler = new FileRepositoryBuilder();
		try {
			this.repository = repobuiler.findGitDir( root ).build();
			this.out = out;
		} catch (IOException e) {
			throw new GitException( e );
		}
	}
	


	@Override
	public void run() throws Exception {
		for (final Ref r : getSortedRefs()) {
			show(r.getObjectId(), r.getName());
			if (r.getPeeledObjectId() != null)
				show(r.getPeeledObjectId(), r.getName() + "^{}");
		}
	}

	private Iterable<Ref> getSortedRefs() {
		Map<String, Ref> all = repository.getAllRefs();
		if (all instanceof RefMap
				|| (all instanceof SortedMap && ((SortedMap) all).comparator() == null))
			return all.values();
		return RefComparator.sort(all.values());
	}

	private void show(final AnyObjectId id, final String name) {
		out.print( id.name() );
		out.print('\t');
		out.print(name);
		out.println();
	}
}

