import React, { Component } from 'react';
import { Text, View, Dimensions } from 'react-native';

const {height,width} = Dimensions.get('window');

import AdvancedWebView from 'advanced-react-native-webview';

export default class App extends Component {

  constructor(){
    super();
    this.src = 'https://www.99xtechnology.com/';
    console.log('test');
  }
  render() {
    return (
      <View style={{ flex: 1,justifyContent: 'center',alignItems: 'center' }} >            
         <AdvancedWebView
             source={{ uri: this.src }}
             style={{ flex: 1,width:width,height:height}}
         />
       </View>
    );
  }
}
