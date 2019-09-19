package org.patdouble.adventuregame

import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.context.SpringBootTest
import spock.lang.Specification

@SpringBootTest
@AutoConfigureTestDatabase
class AdventuregameApplicationTests extends Specification {

	void contextLoads() {
		expect: 'context will load'
	}

}
