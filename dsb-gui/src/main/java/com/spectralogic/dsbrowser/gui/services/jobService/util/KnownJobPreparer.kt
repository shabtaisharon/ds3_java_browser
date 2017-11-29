/*
 * ***************************************************************************
 *   Copyright 2014-2017 Spectra Logic Corporation. All Rights Reserved.
 *   Licensed under the Apache License, Version 2.0 (the "License"). You may not use
 *   this file except in compliance with the License. A copy of the License is located at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *   or in the "license" file accompanying this file.
 *   This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 *   CONDITIONS OF ANY KIND, either express or implied. See the License for the
 *   specific language governing permissions and limitations under the License.
 * ***************************************************************************
 */

package com.spectralogic.dsbrowser.gui.services.jobService.util

import com.google.common.collect.ImmutableMap
import com.spectralogic.ds3client.Ds3Client
import com.spectralogic.ds3client.helpers.Ds3ClientHelpers
import com.spectralogic.dsbrowser.gui.services.jobService.JobData
import com.spectralogic.dsbrowser.gui.services.jobinterruption.FilesAndFolderMap
import java.nio.file.Path
import java.util.*

class KnownJob constructor(
        private val jobData: JobData,
        private val ffm: FilesAndFolderMap,
        val jobId: UUID,
        val client: Ds3Client,
        private val jobType: String
) : JobData by jobData {

    override var job: Ds3ClientHelpers.Job? = null
        get() {
            if (field == null) {
                if (jobType == "GET") {
                    field = Ds3ClientHelpers.wrap(client, 100).recoverReadJob(jobId)
                } else if (jobType == "PUT") {
                    field = Ds3ClientHelpers.wrap(client, 100).recoverWriteJob(jobId)
                }
            }
            jobData.job = field
            return jobData.job
        }

    override var prefixMap: MutableMap<String, Path> = mutableMapOf()
        get() {
            val builder: ImmutableMap.Builder<String, Path> = ImmutableMap.builder()
            builder.putAll(ffm.files)
            builder.putAll(ffm.folders)
            jobData.prefixMap = builder.build()
            return jobData.prefixMap
        }
}