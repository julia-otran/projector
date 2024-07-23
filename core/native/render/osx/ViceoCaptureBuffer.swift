//
//  ViceoCaptureBuffer.swift
//  Projector
//
//  Created by Júlia Otranto Aulicino on 19/07/24.
//

import Foundation
import AVFoundation
import CoreImage

@objc public class VideoCaptureBuffer: NSObject, AVCaptureVideoDataOutputSampleBufferDelegate {
    public override init() {
        
    }
    
    var buffer: CMSampleBuffer?
    let ciContext = CIContext()
    
    public func captureOutput(_ output: AVCaptureOutput, didOutput sampleBuffer: CMSampleBuffer, from connection: AVCaptureConnection) {
        buffer = sampleBuffer
    }
    
    @objc public func printOutput(_ dataBuffer: UnsafeMutableRawPointer) {
        if let sampleBuffer = buffer {
            if let imageBuffer = CMSampleBufferGetImageBuffer(sampleBuffer) {
                let image = CIImage(cvImageBuffer: imageBuffer)
                let rowBytes = Int(image.extent.width * 4)
                let rect = CGRect(x: 0, y: 0, width: image.extent.width, height: image.extent.height)
                
                ciContext.render(image, toBitmap: dataBuffer, rowBytes: rowBytes, bounds: rect, format: .RGBA8, colorSpace: CGColorSpaceCreateDeviceRGB())
            }
        }
    }
    
    @objc public func printOutputPreview(_ dataBuffer: UnsafeMutableRawPointer) {
        if let sampleBuffer = buffer {
            if let imageBuffer = CMSampleBufferGetImageBuffer(sampleBuffer) {
                let image = CIImage(cvImageBuffer: imageBuffer)
                let rowBytes = Int(image.extent.width * 4)
                let rect = CGRect(x: 0, y: 0, width: image.extent.width, height: image.extent.height)
                
                ciContext.render(image, toBitmap: dataBuffer, rowBytes: rowBytes, bounds: rect, format: .BGRA8, colorSpace: CGColorSpaceCreateDeviceRGB())
            }
        }
    }
}

