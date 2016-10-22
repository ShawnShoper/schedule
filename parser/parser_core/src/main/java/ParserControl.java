import org.ho.yaml.Yaml;

import java.io.File;
import java.io.FileNotFoundException;

/**
 * Created by admin on 2016-10-12.
 */
public class ParserControl {
	public void parseyml (String argName) {
	}

	public static void main (String[] args) throws FileNotFoundException {
		System.out.println(Yaml.loadType(new File("parser/parser_core/src/main/resources/testyml.yml"), A.class));
	}
}
