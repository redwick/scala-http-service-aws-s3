package server.cloud

import server.cloud.controls.{CloudFilesControl, NoneControl}


trait CloudManagerControls
   extends NoneControl
      with CloudFilesControl
{}