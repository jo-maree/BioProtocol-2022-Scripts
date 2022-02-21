/* OPTIMISATION OF CELL DETECTION PARAMETERS IN DAPI STAINED TISSUE
*       Dr Jo-Maree Courtney, University of Tasmania, 2022
*       QuPath-0.3.2
*
*  Make sure the channel name is set to DAPI before running script
*
*  This script iteratively runs QuPath's Cell Detection algorithm on
*  the DAPI channel for each threshold chosen.
*
*  Results are saved as a .csv file in a folder called 'Optimisation
*  results' within the project's directory.
*
*  Note: only rectangular annotations are measured - any irregular or
*  points annotations will be ignored. Make sure any rectangle annotations
*  have been named.
*/

import qupath.lib.objects.PathDetectionObject
import qupath.lib.gui.dialogs.Dialogs

def thresholdSteps = Dialogs.showInputDialog("Threshold", "How many thresholds do you want to try?", 3)
def thresholdStart = Dialogs.showInputDialog("Threshold", "Starting threshold?", 100)
def thresholdIncrement = Dialogs.showInputDialog("Threshold", "Threshold increment?", 50)
def expansion = 2

def dir = buildFilePath(PROJECT_BASE_DIR, 'Optimisation Results')
mkdirs(dir)

def filepath = dir + '\\' +'DAPI Results.csv'
File csvFile = new File(filepath)
csvFile.createNewFile()

new File(filepath).withWriter { fw ->
    fw.writeLine("Image,Annotation,Threshold,Sigma,Radius,Cells")

    def project = getProject()
    for (entry in project.getImageList()) {
        def imageData = entry.readImageData()
        def hierarchy = imageData.getHierarchy()
        def annotations = hierarchy.getAnnotationObjects()
        def name = entry.getImageName()

        def detections = hierarchy.getDetectionObjects()
        hierarchy.removeObjects(detections, true)

        for (annotation in annotations) {
            def annotationROI = annotation.getROI()
            def annotationType = annotationROI.getRoiName()

            if (annotationType.startsWith('Rectangle')) {

                hierarchy.getSelectionModel().clearSelection()
                hierarchy.getSelectionModel().setSelectedObject(annotation)

                        for (int th = 0; th < thresholdSteps; th++) {
                            def threshold = thresholdStart + thresholdIncrement * th
                            runPlugin('qupath.imagej.detect.cells.WatershedCellDetection',
                                    imageData,
                                    '{"detectionImage": "DAPI",  ' +
                                            '"requestedPixelSizeMicrons": 0.5,  ' +
                                            '"backgroundRadiusMicrons": 8.0,  ' +
                                            '"medianRadiusMicrons": 0.0,  ' +
                                            '"sigmaMicrons": 1.5,  ' +
                                            '"minAreaMicrons": 10.0,  ' +
                                            '"maxAreaMicrons": 400.0,  ' +
                                            '"threshold": ' + threshold + ',  ' +
                                            '"watershedPostProcess": true,  ' +
                                            '"cellExpansionMicrons": ' + expansion + ',  ' +
                                            '"includeNuclei": true,  ' +
                                            '"smoothBoundaries": true,  ' +
                                            '"makeMeasurements": true}')


                            def objects = hierarchy.getObjectsForROI(null, annotation.getROI()).findAll {
                                it.isDetection()
                            }
                            hierarchy.getObjectsForROI(PathDetectionObject, annotation.getROI())
                            def detectionCount = objects.size()
                            def data = name + ',' +
                                    annotation.getName() + ',' +
                                    Double.toString(threshold) + ',' +
                                    Double.toString(sigma) + ',' +
                                    Double.toString(radius) + ',' +
                                    Double.toString(detectionCount)
                            fw.writeLine(data)
                        }
            }
        }

    }
}
print 'DONE!'

Dialogs.showPlainMessage('Optimisation', "Optimisation results saved.")
