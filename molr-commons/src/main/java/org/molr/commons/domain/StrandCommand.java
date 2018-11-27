/**
 * Copyright (c) 2018 European Organisation for Nuclear Research (CERN), All Rights Reserved.
 */

package org.molr.commons.domain;

public enum StrandCommand {
    PAUSE, STEP_OVER, STEP_INTO, SKIP, RESUME;

    /* Is skip really a useful command, or should we rather introduce an attribute of a block 'skipped' ?*/
}
