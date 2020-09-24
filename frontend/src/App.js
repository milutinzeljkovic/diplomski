import React, { Component } from 'react';
import './App.css';
import Workers from './Workers/Workers';

class App extends Component {
  render() {
    return (
      <div className="App">
        <div className="header">
          <h2>Working hours evidence</h2>
        </div>
        <Workers/>
      </div>
    );
  }
}

export default App;
