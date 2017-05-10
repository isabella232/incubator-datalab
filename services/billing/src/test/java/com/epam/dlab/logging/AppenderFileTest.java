/***************************************************************************

Copyright (c) 2016, EPAM SYSTEMS INC

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

****************************************************************************/

package com.epam.dlab.logging;

import static junit.framework.TestCase.assertEquals;

import org.junit.Test;

public class AppenderFileTest {

	@Test
	public void config() {
		AppenderFile appender = new AppenderFile();
		appender.setArchive(true);
		appender.setArchivedFileCount(123);
		appender.setArchivedLogFilenamePattern("file.log.zip");
		appender.setCurrentLogFilename("file.log");
		
		assertEquals("file", appender.getType());
		assertEquals(true, appender.getArchive());
		assertEquals(123, appender.getArchivedFileCount());
		assertEquals("file.log.zip", appender.getArchivedLogFilenamePattern());
		assertEquals("file.log", appender.getCurrentLogFilename());
	}
}
