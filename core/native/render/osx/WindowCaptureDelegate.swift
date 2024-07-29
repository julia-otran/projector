//
//  WindowCaptureDelegate.swift
//  Projector
//
//  Created by Júlia Otranto Aulicino on 28/07/24.
//

import Foundation
import ScreenCaptureKit

@objc public class WindowCaptureDelegate : NSObject, SCStreamDelegate, SCStreamOutput {
    public override init() {
        
    }
    
    var buffer: CMSampleBuffer?
    let ciContext = CIContext()
    
    public func stream(_ stream: SCStream, didStopWithError error: Error) {
        buffer = nil
    }
    
    public func stream(_ stream: SCStream, didOutputSampleBuffer sampleBuffer: CMSampleBuffer, of type: SCStreamOutputType) {
        buffer = sampleBuffer
    }
    
    @objc public func printScreen(_ dataBuffer: UnsafeMutableRawPointer, width: Int, height: Int) {
        if let sampleBuffer = buffer {
            if let imageBuffer = CMSampleBufferGetImageBuffer(sampleBuffer) {
                let image = CIImage(cvImageBuffer: imageBuffer)
                
                let area = image.extent.width * image.extent.height
                
                if (Int(area) <= width * height) {
                    let rowBytes = Int(image.extent.width * 4)
                    let rect = CGRect(x: 0, y: 0, width: image.extent.width, height: image.extent.height)
                    
                    ciContext.render(image, toBitmap: dataBuffer, rowBytes: rowBytes, bounds: rect, format: .RGBA8, colorSpace: CGColorSpaceCreateDeviceRGB())
                }
            }
        }
    }
}
