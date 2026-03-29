Pod::Spec.new do |s|
  s.name         = 'VeepooBleSDK'
  s.version      = '2.2.0'
  s.summary      = 'HBandSDK / VeepooBleSDK iOS framework for G Band devices'
  s.homepage     = 'https://github.com/HBandSDK/iOS_Ble_SDK'
  s.license      = { :type => 'Proprietary' }
  s.author       = { 'HBandSDK' => 'https://github.com/HBandSDK' }
  s.platform     = :ios, '13.0'
  s.source       = { :path => '.' }
  s.vendored_frameworks = 'VeepooBleSDK.framework'
  s.frameworks   = 'CoreBluetooth', 'Foundation'
  s.requires_arc = true
end
